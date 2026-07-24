package com.arkapp.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arkapp.AppState
import com.arkapp.i18n.LocalStrings
import com.arkapp.ini.ProfileRepository
import com.arkapp.steam.SteamProcess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.nio.file.AccessDeniedException
import java.nio.file.Path
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

private val dateFormat = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")

private data class EditorState(
    val profileId: String?,   // null = creating a new profile
    val name: String,
    val content: String,
)

@Composable
fun ProfilesTab(state: AppState) {
    val strings = LocalStrings.current
    val scope = rememberCoroutineScope()
    val settings by state.settings.state.collectAsState()

    var profilesList by remember { mutableStateOf<List<ProfileRepository.ProfileMeta>>(emptyList()) }
    var activeState by remember { mutableStateOf<Pair<ProfileRepository.ProfileMeta, ProfileRepository.ActiveState>?>(null) }
    var nameText by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<Pair<String, Boolean>?>(null) }
    var confirmApply by remember { mutableStateOf<ProfileRepository.ProfileMeta?>(null) }
    var confirmDelete by remember { mutableStateOf<ProfileRepository.ProfileMeta?>(null) }
    var editor by remember { mutableStateOf<EditorState?>(null) }

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
            }
        }
    }

    LaunchedEffect(Unit) { refresh() }

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
        if (SteamProcess.isArkRunning()) {
            message = strings.profArkRunning to true
            return
        }
        runGuarded {
            withContext(Dispatchers.IO) { state.profiles.apply(profile) }
            message = strings.profAppliedOk(profile.name) to false
            refresh()
        }
    }

    editor?.let { ed ->
        Column(
            Modifier.fillMaxSize().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            message?.let { (text, isError) -> StatusMessage(text, isError) { message = null } }
            Text(
                strings.profEditorTitle,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            OutlinedTextField(
                value = ed.name,
                onValueChange = { editor = ed.copy(name = it) },
                placeholder = { Text(strings.profNamePlaceholder) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = ed.content,
                onValueChange = { editor = ed.copy(content = it) },
                placeholder = { Text(strings.profEditorContentPlaceholder) },
                textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, fontSize = 13.sp),
                modifier = Modifier.weight(1f).fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    enabled = ed.name.isNotBlank() && !busy,
                    onClick = {
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
                    },
                ) { Text(strings.profSave) }
                TextButton(onClick = { editor = null }) { Text(strings.cancel) }
            }
        }
        return
    }

    Column(
        Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        message?.let { (text, isError) -> StatusMessage(text, isError) { message = null } }

        InfoBanner(
            strings.profIntegrityWarning,
            icon = Icons.Default.Warning,
            modifier = Modifier.fillMaxWidth(),
        )
        if (!arkOk) {
            InfoBanner(
                strings.profArkNotFound,
                icon = Icons.Default.Warning,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        SectionCard(strings.profNewTitle) {
            Text(
                strings.profNewHelp,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.size(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = nameText,
                    onValueChange = { nameText = it },
                    placeholder = { Text(strings.profNamePlaceholder) },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(12.dp))
                Button(
                    enabled = nameText.isNotBlank() && !busy && arkOk,
                    onClick = {
                        val name = nameText.trim()
                        runGuarded {
                            withContext(Dispatchers.IO) { state.profiles.saveCurrentAs(name) }
                            message = strings.profSavedOk(name) to false
                            nameText = ""
                            refresh()
                        }
                    },
                ) { Text(strings.profSaveCurrent) }
            }
            Spacer(Modifier.size(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    enabled = !busy,
                    onClick = {
                        pickIniFile(strings.profImport)?.let { picked ->
                            runGuarded {
                                val content = withContext(Dispatchers.IO) {
                                    ProfileRepository.readTextLenient(picked)
                                }
                                editor = EditorState(
                                    profileId = null,
                                    name = picked.fileName.toString().removeSuffix(".ini"),
                                    content = content,
                                )
                            }
                        }
                    },
                ) { Text(strings.profImport) }
                OutlinedButton(
                    enabled = !busy,
                    onClick = { editor = EditorState(profileId = null, name = "", content = "") },
                ) { Text(strings.profCreateNew) }
            }
        }

        SectionCard(strings.profListTitle, modifier = Modifier.weight(1f).fillMaxWidth()) {
            if (profilesList.isEmpty()) {
                Text(
                    strings.profEmpty,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(profilesList, key = { it.id }) { profile ->
                        val isActive = activeState?.first?.id == profile.id
                        val activeLabel = when {
                            isActive && activeState?.second == ProfileRepository.ActiveState.ACTIVE -> strings.profActive
                            isActive -> strings.profActiveModified
                            else -> null
                        }
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            profile.name,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.SemiBold,
                                        )
                                        if (activeLabel != null) {
                                            Spacer(Modifier.width(8.dp))
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = SuccessGreen.copy(alpha = 0.18f),
                                            ) {
                                                Text(
                                                    activeLabel,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = SuccessGreen,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                )
                                            }
                                        }
                                    }
                                    Text(
                                        Instant.ofEpochMilli(profile.createdAt)
                                            .atZone(ZoneId.systemDefault()).format(dateFormat),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                IconButton(
                                    enabled = !busy,
                                    onClick = {
                                        runGuarded {
                                            val content = withContext(Dispatchers.IO) {
                                                state.profiles.readContent(profile)
                                            }
                                            editor = EditorState(profile.id, profile.name, content)
                                        }
                                    },
                                ) {
                                    Icon(
                                        Icons.Default.Edit,
                                        contentDescription = strings.profEdit,
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                                Spacer(Modifier.width(4.dp))
                                if (isActive) {
                                    OutlinedButton(
                                        enabled = !busy && arkOk,
                                        onClick = { applyProfile(profile) },
                                    ) { Text(strings.profReapply) }
                                } else {
                                    Button(
                                        enabled = !busy && arkOk,
                                        onClick = { confirmApply = profile },
                                    ) { Text(strings.profApply) }
                                }
                                Spacer(Modifier.width(4.dp))
                                IconButton(enabled = !busy, onClick = { confirmDelete = profile }) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = strings.profDelete,
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }
            TextButton(
                enabled = !busy && arkOk,
                onClick = {
                    runGuarded {
                        val restored = withContext(Dispatchers.IO) { state.profiles.restoreLastBackup() }
                        message = if (restored) strings.profRestoredOk to false else strings.profNoBackup to true
                        refresh()
                    }
                },
            ) { Text(strings.profRestoreBackup) }
        }
    }

    confirmApply?.let { profile ->
        ConfirmDialog(
            title = strings.profApplyConfirmTitle,
            body = strings.profApplyConfirmBody(profile.name),
            confirmLabel = strings.profApply,
            dismissLabel = strings.cancel,
            onConfirm = {
                confirmApply = null
                applyProfile(profile)
            },
            onDismiss = { confirmApply = null },
        )
    }

    confirmDelete?.let { profile ->
        ConfirmDialog(
            title = strings.profDeleteConfirmTitle,
            body = strings.profDeleteConfirmBody(profile.name),
            confirmLabel = strings.profDelete,
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

private fun pickIniFile(title: String): Path? {
    val chooser = JFileChooser().apply {
        dialogTitle = title
        fileSelectionMode = JFileChooser.FILES_ONLY
        fileFilter = FileNameExtensionFilter("INI", "ini")
        isAcceptAllFileFilterUsed = true
    }
    return if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
        chooser.selectedFile.toPath()
    } else null
}
