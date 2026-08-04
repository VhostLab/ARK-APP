package com.arkapp.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arkapp.AppState
import com.arkapp.i18n.LocalStrings
import com.arkapp.ini.ProfileRepository
import com.arkapp.steam.SteamProcess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.awt.FileDialog
import java.awt.Frame
import java.nio.file.AccessDeniedException
import java.nio.file.Path

private data class EditorState(
    val profileId: String?,   // null = creating a new profile
    val name: String,
    val content: String,
)

/** Add-profile wizard state. Step: 1 origin, 2 details, 3 done. */
private data class ProfWiz(
    val step: Int = 1,
    val mode: String = "snapshot",   // snapshot | import | new
    val name: String = "",
    val file: Path? = null,
    val doneName: String = "",
)

@Composable
fun ProfilesTab(state: AppState) {
    val strings = LocalStrings.current
    val scope = rememberCoroutineScope()
    val settings by state.settings.state.collectAsState()

    var profilesList by remember { mutableStateOf<List<ProfileRepository.ProfileMeta>>(emptyList()) }
    var activeState by remember { mutableStateOf<Pair<ProfileRepository.ProfileMeta, ProfileRepository.ActiveState>?>(null) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<Pair<String, Boolean>?>(null) }
    var confirmApply by remember { mutableStateOf<Pair<ProfileRepository.ProfileMeta, Boolean>?>(null) }
    var confirmDelete by remember { mutableStateOf<ProfileRepository.ProfileMeta?>(null) }
    var editor by remember { mutableStateOf<EditorState?>(null) }
    var profWiz by remember { mutableStateOf<ProfWiz?>(null) }

    val arkOk = remember(settings) { state.arkLocator.arkRoot() != null }

    fun refresh() {
        scope.launch {
            withContext(Dispatchers.IO) {
                val list = state.profiles.list()
                val active = runCatching { state.profiles.activeState() }.getOrNull()
                list to active
            }.let { (list, active) ->
                profilesList = list
                activeState = active
                state.setupProfileDone.value = active != null
            }
        }
    }

    LaunchedEffect(Unit) { refresh() }

    // Success messages fade out on their own; errors stay until dismissed.
    LaunchedEffect(message) {
        val current = message
        if (current != null && !current.second) {
            delay(5000)
            if (message == current) message = null
        }
    }

    fun mapError(e: Exception): String = when {
        e is AccessDeniedException -> strings.errorAccessDenied
        e.message == "ARK_NOT_FOUND" -> strings.profArkNotFound
        e.message == "INI_NOT_FOUND" -> strings.profFilesMissing
        else -> strings.errorGeneric(e.message ?: e.toString())
    }

    fun runGuarded(block: suspend () -> Unit) {
        scope.launch {
            busy = true
            try {
                block()
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                message = mapError(e) to true
            } finally {
                busy = false
            }
        }
    }

    fun applyProfile(profile: ProfileRepository.ProfileMeta) {
        runGuarded {
            withContext(Dispatchers.IO) { state.profiles.apply(profile) }
            message = strings.profAppliedOk(profile.name) to false
            refresh()
        }
    }

    fun askApply(profile: ProfileRepository.ProfileMeta) {
        scope.launch {
            val running = withContext(Dispatchers.IO) { SteamProcess.isArkRunning() }
            confirmApply = profile to running
        }
    }

    // ── Editor takes over the whole tab ──
    editor?.let { ed ->
        Column(
            Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            message?.let { (text, isError) -> StatusMessage(text, isError) { message = null } }
            Text(
                if (ed.profileId != null) strings.profEditorTitleEdit else strings.profEditorTitleNew,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = Palette.Text,
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(strings.profNameLabel, fontSize = 12.5.sp, color = Palette.Muted)
                AppTextField(
                    value = ed.name,
                    onValueChange = { editor = ed.copy(name = it) },
                    placeholder = strings.profNamePlaceholder,
                    singleLine = true,
                    modifier = Modifier.width(320.dp),
                )
            }
            AppTextField(
                value = ed.content,
                onValueChange = { editor = ed.copy(content = it) },
                placeholder = strings.profEditorContentPlaceholder,
                mono = true,
                modifier = Modifier.weight(1f).fillMaxWidth(),
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                GhostButton(strings.cancel) { editor = null }
                Spacer(Modifier.width(8.dp))
                PrimaryButton(strings.save, enabled = ed.name.isNotBlank() && !busy) {
                    val name = ed.name.trim()
                    runGuarded {
                        withContext(Dispatchers.IO) {
                            val existing = ed.profileId?.let { id -> profilesList.firstOrNull { it.id == id } }
                            if (existing == null) {
                                state.profiles.createProfile(name, ed.content)
                            } else {
                                state.profiles.updateProfile(existing, name, ed.content)
                            }
                        }
                        message = strings.profSavedOk(name) to false
                        editor = null
                        refresh()
                    }
                }
            }
        }
        return
    }

    Column(
        Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        message?.let { (text, isError) -> StatusMessage(text, isError) { message = null } }

        if (!arkOk) {
            WarnBanner(strings.profArkNotFound, Modifier.fillMaxWidth())
        }

        Text(strings.tabProfiles, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = Palette.Text)

        AppCard(Modifier.weight(1f).fillMaxWidth()) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.weight(1f)) { CardTitle(strings.profListTitle, profilesList.size) }
                PrimaryButton("＋ ${strings.profAddProfile}") { profWiz = ProfWiz() }
            }

            if (profilesList.isEmpty()) {
                HairLine()
                Box(Modifier.weight(1f).fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(strings.profEmptyTitle, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Palette.Text)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            strings.profEmptyBody,
                            fontSize = 13.sp,
                            color = Palette.Muted,
                            lineHeight = 20.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.width(380.dp),
                        )
                        Spacer(Modifier.height(14.dp))
                        PrimaryButton("＋ ${strings.profAddProfile}") { profWiz = ProfWiz() }
                    }
                }
            } else {
                LazyColumn(Modifier.weight(1f).fillMaxWidth()) {
                    items(profilesList, key = { it.id }) { profile ->
                        HairLine(0.05f)
                        val isActive = activeState?.first?.id == profile.id
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 11.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            // State circle: green check = active, red X = not active
                            val stateColor = if (isActive) Palette.Success else Palette.DangerSoft
                            Surface(
                                shape = CircleShape,
                                color = stateColor.copy(alpha = 0.16f),
                                border = BorderStroke(1.dp, stateColor.copy(alpha = 0.5f)),
                                modifier = Modifier.size(20.dp),
                            ) {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Icon(
                                        if (isActive) Icons.Default.Check else Icons.Default.Close,
                                        contentDescription = if (isActive) strings.profActive else null,
                                        tint = stateColor,
                                        modifier = Modifier.size(12.dp),
                                    )
                                }
                            }
                            Text(
                                profile.name,
                                fontSize = 13.5.sp,
                                color = Palette.Text,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                            )
                            GhostButton(strings.profEdit, enabled = !busy, width = 80.dp) {
                                runGuarded {
                                    val content = withContext(Dispatchers.IO) { state.profiles.readContent(profile) }
                                    editor = EditorState(profile.id, profile.name, content)
                                }
                            }
                            AccentGhostButton(
                                if (isActive) strings.profReapply else strings.profApply,
                                enabled = !busy && arkOk,
                                width = 100.dp,
                            ) { askApply(profile) }
                            DangerGhostButton(strings.profDelete, enabled = !busy, width = 80.dp) {
                                confirmDelete = profile
                            }
                        }
                    }
                }
                HairLine()
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
                    TextButton(
                        enabled = !busy && arkOk,
                        onClick = {
                            runGuarded {
                                val restored = withContext(Dispatchers.IO) { state.profiles.restoreLastBackup() }
                                message = if (restored) strings.profRestoredOk to false else strings.profNoBackup to true
                                refresh()
                            }
                        },
                    ) {
                        Text(strings.profRestoreBackup, fontSize = 12.5.sp, color = LocalAccent.current.hi)
                    }
                }
            }
        }
    }

    // ══ Add-profile wizard ══
    profWiz?.let { wiz ->
        AppModal(width = 560.dp, onDismiss = { profWiz = null }) {
            ModalHeader(strings.profWizTitle) { profWiz = null }
            WizardSteps(wiz.step, listOf(strings.profWizOrigin, strings.profWizDetails, strings.wizDone))
            when (wiz.step) {
                1 -> Column(
                    Modifier.padding(start = 20.dp, end = 20.dp, bottom = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(strings.profWizIntro, fontSize = 12.5.sp, color = Palette.Muted, lineHeight = 19.sp)
                    Row(
                        Modifier.height(IntrinsicSize.Max),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        ModeCard(
                            selected = wiz.mode == "snapshot",
                            emoji = "🎮",
                            title = strings.profModeSnapshot,
                            desc = strings.profModeSnapshotDesc,
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                        ) { profWiz = wiz.copy(mode = "snapshot") }
                        ModeCard(
                            selected = wiz.mode == "import",
                            emoji = "📄",
                            title = strings.profModeImport,
                            desc = strings.profModeImportDesc,
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                        ) { profWiz = wiz.copy(mode = "import") }
                        ModeCard(
                            selected = wiz.mode == "new",
                            emoji = "✏️",
                            title = strings.profModeNew,
                            desc = strings.profModeNewDesc,
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                        ) { profWiz = wiz.copy(mode = "new") }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        GhostButton(strings.cancel) { profWiz = null }
                        Spacer(Modifier.width(8.dp))
                        PrimaryButton(strings.next) { profWiz = wiz.copy(step = 2) }
                    }
                }

                2 -> Column(
                    Modifier.padding(start = 20.dp, end = 20.dp, bottom = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (wiz.mode == "import") {
                        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Text(strings.profFileLabel, fontSize = 12.sp, color = Palette.Muted)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                AppTextField(
                                    value = wiz.file?.toString() ?: "",
                                    onValueChange = {},
                                    readOnly = true,
                                    singleLine = true,
                                    mono = true,
                                    modifier = Modifier.weight(1f),
                                )
                                GhostButton(strings.setBrowse) {
                                    pickIniFile(strings.profModeImport)?.let { picked ->
                                        val suggested = wiz.name.ifBlank {
                                            picked.fileName.toString().removeSuffix(".ini")
                                        }
                                        profWiz = wiz.copy(file = picked, name = suggested)
                                    }
                                }
                            }
                        }
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text(strings.profNameLabel, fontSize = 12.sp, color = Palette.Muted)
                        AppTextField(
                            value = wiz.name,
                            onValueChange = { profWiz = wiz.copy(name = it) },
                            placeholder = strings.profNamePlaceholder,
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    Text(
                        when (wiz.mode) {
                            "snapshot" -> strings.profHintSnapshot
                            "import" -> strings.profHintImport
                            else -> strings.profHintNew
                        },
                        fontSize = 12.sp,
                        color = Palette.Dim,
                        lineHeight = 18.sp,
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        GhostButton(strings.back) { profWiz = wiz.copy(step = 1) }
                        val canFinish = wiz.name.isNotBlank() &&
                            (wiz.mode != "import" || wiz.file != null) &&
                            (wiz.mode == "new" || arkOk || wiz.mode == "import")
                        PrimaryButton(
                            when (wiz.mode) {
                                "import" -> strings.profFinishImport
                                "new" -> strings.profFinishEditor
                                else -> strings.save
                            },
                            enabled = canFinish && !busy,
                        ) {
                            val name = wiz.name.trim()
                            when (wiz.mode) {
                                "new" -> {
                                    profWiz = null
                                    editor = EditorState(profileId = null, name = name, content = "[ScalabilityGroups]\n")
                                }
                                "import" -> {
                                    val file = wiz.file ?: return@PrimaryButton
                                    runGuarded {
                                        withContext(Dispatchers.IO) { state.profiles.importFile(name, file) }
                                        profWiz = wiz.copy(step = 3, doneName = name)
                                        refresh()
                                    }
                                }
                                else -> {
                                    runGuarded {
                                        withContext(Dispatchers.IO) { state.profiles.saveCurrentAs(name) }
                                        profWiz = wiz.copy(step = 3, doneName = name)
                                        refresh()
                                    }
                                }
                            }
                        }
                    }
                }

                3 -> Column(
                    Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, bottom = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    DoneCircle()
                    Text(
                        if (wiz.mode == "import") strings.profDoneImported(wiz.doneName)
                        else strings.profDoneSaved(wiz.doneName),
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Palette.Text,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        strings.profDoneNote,
                        fontSize = 12.5.sp,
                        color = Palette.Muted,
                        lineHeight = 19.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.width(380.dp),
                    )
                    PrimaryButton(strings.close) { profWiz = null }
                }
            }
        }
    }

    confirmApply?.let { (profile, arkRunning) ->
        ConfirmModal(
            title = strings.profApplyConfirmTitle(profile.name),
            body = strings.profApplyBody,
            confirmLabel = strings.profApply,
            confirmEnabled = !arkRunning,
            warning = if (arkRunning) strings.profArkRunningWarn else null,
            dismissLabel = strings.cancel,
            onConfirm = {
                confirmApply = null
                applyProfile(profile)
            },
            onDismiss = { confirmApply = null },
        )
    }

    confirmDelete?.let { profile ->
        ConfirmModal(
            title = strings.profDeleteConfirmTitle(profile.name),
            body = strings.profDeleteBody,
            confirmLabel = strings.profDeleteConfirm,
            danger = true,
            dismissLabel = strings.cancel,
            onConfirm = {
                confirmDelete = null
                runGuarded {
                    withContext(Dispatchers.IO) { state.profiles.delete(profile) }
                    message = strings.profDeletedOk(profile.name) to false
                    refresh()
                }
            },
            onDismiss = { confirmDelete = null },
        )
    }
}

/** Card option inside the add-profile wizard. */
@Composable
private fun ModeCard(
    selected: Boolean,
    emoji: String,
    title: String,
    desc: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val accent = LocalAccent.current
    Surface(
        onClick = onClick,
        shape = CardShape,
        color = Palette.Bg,
        border = if (selected) BorderStroke(1.dp, accent.base) else BorderStroke(1.dp, Palette.Border12),
        modifier = modifier,
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(emoji, fontSize = 17.sp)
            Text(title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Palette.Text)
            Text(desc, fontSize = 11.5.sp, color = Palette.Muted, lineHeight = 16.sp)
        }
    }
}

/** Native Windows open-file dialog (AWT FileDialog, not the Swing one). */
private fun pickIniFile(title: String): Path? {
    val parent = Frame.getFrames().firstOrNull { it.isVisible }
    val dialog = FileDialog(parent, title, FileDialog.LOAD)
    dialog.file = "*.ini"
    dialog.isVisible = true
    val file = dialog.file ?: return null
    return Path.of(dialog.directory, file)
}
