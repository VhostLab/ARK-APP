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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.arkapp.AppState
import com.arkapp.i18n.LocalStrings
import com.arkapp.ini.ProfileRepository
import com.arkapp.steam.SteamProcess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.nio.file.AccessDeniedException
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val dateFormat = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")

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

        SectionCard(strings.profSaveCurrentTitle) {
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
                ) { Text(strings.profSave) }
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
