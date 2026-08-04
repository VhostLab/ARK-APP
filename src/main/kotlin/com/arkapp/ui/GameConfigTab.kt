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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

/** Add-profile wizard state for a config section. Step: 1 origin, 2 details, 3 done. */
private data class GcWiz(
    val step: Int = 1,
    val mode: String = "snapshot",   // snapshot | import
    val name: String = "",
    val file: Path? = null,
    val doneName: String = "",
)

@Composable
fun GameConfigTab(state: AppState) {
    val strings = LocalStrings.current
    var message by remember { mutableStateOf<Pair<String, Boolean>?>(null) }

    val settingsSnapshot = state.settings.value
    val arkOk = remember(settingsSnapshot) { state.arkLocator.arkRoot() != null }

    // Success messages fade out on their own; errors stay until dismissed.
    LaunchedEffect(message) {
        val current = message
        if (current != null && !current.second) {
            delay(5000)
            if (message == current) message = null
        }
    }

    Column(
        Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        message?.let { (text, isError) -> StatusMessage(text, isError) { message = null } }

        if (!arkOk) {
            WarnBanner(strings.profArkNotFound, Modifier.fillMaxWidth())
        }

        Text(strings.tabGameConfig, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = Palette.Text)

        ConfigSection(
            state = state,
            repo = state.gusProfiles,
            title = strings.gcVisualTitle,
            fileLabel = "GameUserSettings.ini",
            arkOk = arkOk,
            onMessage = { message = it },
            modifier = Modifier.weight(1f).fillMaxWidth(),
        )
        ConfigSection(
            state = state,
            repo = state.inputProfiles,
            title = strings.gcKeysTitle,
            fileLabel = "Input.ini",
            arkOk = arkOk,
            onMessage = { message = it },
            modifier = Modifier.weight(1f).fillMaxWidth(),
        )
    }
}

@Composable
private fun ConfigSection(
    state: AppState,
    repo: ProfileRepository,
    title: String,
    fileLabel: String,
    arkOk: Boolean,
    onMessage: (Pair<String, Boolean>?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalStrings.current
    val scope = rememberCoroutineScope()

    var profilesList by remember { mutableStateOf<List<ProfileRepository.ProfileMeta>>(emptyList()) }
    var activeState by remember { mutableStateOf<Pair<ProfileRepository.ProfileMeta, ProfileRepository.ActiveState>?>(null) }
    var busy by remember { mutableStateOf(false) }
    var wiz by remember { mutableStateOf<GcWiz?>(null) }
    var confirmApply by remember { mutableStateOf<Pair<ProfileRepository.ProfileMeta, Boolean>?>(null) }
    var confirmDelete by remember { mutableStateOf<ProfileRepository.ProfileMeta?>(null) }

    fun refresh() {
        scope.launch {
            withContext(Dispatchers.IO) {
                val list = repo.list()
                val active = runCatching { repo.activeState() }.getOrNull()
                list to active
            }.let { (list, active) ->
                profilesList = list
                activeState = active
            }
        }
    }

    LaunchedEffect(Unit) { refresh() }

    fun mapError(e: Exception): String = when {
        e is AccessDeniedException -> strings.errorAccessDenied
        e.message == "ARK_NOT_FOUND" -> strings.profArkNotFound
        e.message == "INI_NOT_FOUND" -> strings.gcFileMissing(fileLabel)
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
                onMessage(mapError(e) to true)
            } finally {
                busy = false
            }
        }
    }

    AppCard(modifier) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                CardTitle(title, profilesList.size)
                Text(fileLabel, fontSize = 11.sp, color = Palette.Dim, fontFamily = FontFamily.Monospace)
            }
            PrimaryButton("＋ ${strings.profAddProfile}") { wiz = GcWiz() }
        }

        if (profilesList.isEmpty()) {
            HairLine()
            Box(Modifier.weight(1f).fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) {
                Text(
                    strings.gcEmptyBody(fileLabel),
                    fontSize = 12.5.sp,
                    color = Palette.Muted,
                    lineHeight = 19.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.width(420.dp),
                )
            }
        } else {
            LazyColumn(Modifier.weight(1f).fillMaxWidth()) {
                items(profilesList, key = { it.id }) { profile ->
                    HairLine(0.05f)
                    val isActive = activeState?.first?.id == profile.id
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
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
                        AccentGhostButton(
                            if (isActive) strings.profReapply else strings.profApply,
                            enabled = !busy && arkOk,
                            width = 100.dp,
                        ) {
                            scope.launch {
                                val running = withContext(Dispatchers.IO) { SteamProcess.isArkRunning() }
                                confirmApply = profile to running
                            }
                        }
                        DangerGhostButton(strings.profDelete, enabled = !busy, width = 80.dp) {
                            confirmDelete = profile
                        }
                    }
                }
            }
            HairLine()
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 2.dp)) {
                TextButton(
                    enabled = !busy && arkOk,
                    onClick = {
                        runGuarded {
                            val restored = withContext(Dispatchers.IO) { repo.restoreLastBackup() }
                            onMessage(if (restored) strings.profRestoredOk to false else strings.profNoBackup to true)
                            refresh()
                        }
                    },
                ) {
                    Text(strings.profRestoreBackup, fontSize = 12.5.sp, color = LocalAccent.current.hi)
                }
            }
        }
    }

    // ══ Add wizard (snapshot / import) ══
    wiz?.let { w ->
        AppModal(width = 560.dp, onDismiss = { wiz = null }) {
            ModalHeader("${strings.profAddProfile} — $title") { wiz = null }
            WizardSteps(w.step, listOf(strings.profWizOrigin, strings.profWizDetails, strings.wizDone))
            when (w.step) {
                1 -> Column(
                    Modifier.padding(start = 20.dp, end = 20.dp, bottom = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(strings.gcWizIntro(fileLabel), fontSize = 12.5.sp, color = Palette.Muted, lineHeight = 19.sp)
                    Row(
                        Modifier.height(IntrinsicSize.Max),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        GcModeCard(
                            selected = w.mode == "snapshot",
                            emoji = "🎮",
                            title = strings.gcModeSnapshot,
                            desc = strings.gcModeSnapshotDesc,
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                        ) { wiz = w.copy(mode = "snapshot") }
                        GcModeCard(
                            selected = w.mode == "import",
                            emoji = "📄",
                            title = strings.profModeImport,
                            desc = strings.profModeImportDesc,
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                        ) { wiz = w.copy(mode = "import") }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        GhostButton(strings.cancel) { wiz = null }
                        Spacer(Modifier.width(8.dp))
                        PrimaryButton(strings.next) { wiz = w.copy(step = 2) }
                    }
                }

                2 -> Column(
                    Modifier.padding(start = 20.dp, end = 20.dp, bottom = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (w.mode == "import") {
                        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Text(strings.profFileLabel, fontSize = 12.sp, color = Palette.Muted)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                AppTextField(
                                    value = w.file?.toString() ?: "",
                                    onValueChange = {},
                                    readOnly = true,
                                    singleLine = true,
                                    mono = true,
                                    modifier = Modifier.weight(1f),
                                )
                                GhostButton(strings.setBrowse) {
                                    pickGcIniFile(strings.profModeImport)?.let { picked ->
                                        val suggested = w.name.ifBlank {
                                            picked.fileName.toString().removeSuffix(".ini")
                                        }
                                        wiz = w.copy(file = picked, name = suggested)
                                    }
                                }
                            }
                        }
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text(strings.profNameLabel, fontSize = 12.sp, color = Palette.Muted)
                        AppTextField(
                            value = w.name,
                            onValueChange = { wiz = w.copy(name = it) },
                            placeholder = strings.profNamePlaceholder,
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    Text(
                        if (w.mode == "snapshot") strings.profHintSnapshot else strings.profHintImport,
                        fontSize = 12.sp,
                        color = Palette.Dim,
                        lineHeight = 18.sp,
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        GhostButton(strings.back) { wiz = w.copy(step = 1) }
                        val canFinish = w.name.isNotBlank() &&
                            (w.mode != "import" || w.file != null) &&
                            (w.mode != "snapshot" || arkOk)
                        PrimaryButton(
                            if (w.mode == "import") strings.profFinishImport else strings.save,
                            enabled = canFinish && !busy,
                        ) {
                            val name = w.name.trim()
                            if (w.mode == "import") {
                                val file = w.file ?: return@PrimaryButton
                                runGuarded {
                                    withContext(Dispatchers.IO) { repo.importFile(name, file) }
                                    wiz = w.copy(step = 3, doneName = name)
                                    refresh()
                                }
                            } else {
                                runGuarded {
                                    withContext(Dispatchers.IO) { repo.saveCurrentAs(name) }
                                    wiz = w.copy(step = 3, doneName = name)
                                    refresh()
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
                        if (w.mode == "import") strings.profDoneImported(w.doneName)
                        else strings.profDoneSaved(w.doneName),
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
                    PrimaryButton(strings.close) { wiz = null }
                }
            }
        }
    }

    confirmApply?.let { (profile, arkRunning) ->
        ConfirmModal(
            title = strings.profApplyConfirmTitle(profile.name),
            body = strings.gcApplyBody(fileLabel),
            confirmLabel = strings.profApply,
            confirmEnabled = !arkRunning,
            warning = if (arkRunning) strings.profArkRunningWarn else null,
            dismissLabel = strings.cancel,
            onConfirm = {
                confirmApply = null
                runGuarded {
                    withContext(Dispatchers.IO) { repo.apply(profile) }
                    onMessage(strings.profAppliedOk(profile.name) to false)
                    refresh()
                }
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
                    withContext(Dispatchers.IO) { repo.delete(profile) }
                    onMessage(strings.profDeletedOk(profile.name) to false)
                    refresh()
                }
            },
            onDismiss = { confirmDelete = null },
        )
    }
}

@Composable
private fun GcModeCard(
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

private fun pickGcIniFile(title: String): Path? {
    val parent = Frame.getFrames().firstOrNull { it.isVisible }
    val dialog = FileDialog(parent, title, FileDialog.LOAD)
    dialog.file = "*.ini"
    dialog.isVisible = true
    val file = dialog.file ?: return null
    return Path.of(dialog.directory, file)
}
