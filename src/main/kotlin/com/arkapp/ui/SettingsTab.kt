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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.arkapp.AppState
import com.arkapp.i18n.LocalStrings
import java.nio.file.Path
import javax.swing.JFileChooser

@Composable
fun SettingsTab(state: AppState) {
    val strings = LocalStrings.current
    val settings by state.settings.state.collectAsState()
    var message by remember { mutableStateOf<Pair<String, Boolean>?>(null) }

    val steamRoot = remember(settings) { state.steamLocator.steamRoot() }
    val steamValid = remember(settings) { state.steamLocator.isValidSteamRoot(steamRoot) }
    val arkRoot = remember(settings) { state.arkLocator.arkRoot() }
    val currentLanguage = settings.language ?: systemLanguage()

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        message?.let { (text, isError) -> StatusMessage(text, isError) { message = null } }

        if (arkRoot == null) {
            SectionCard(strings.firstRunTitle) {
                InfoBanner(
                    strings.firstRunBody,
                    icon = Icons.Default.Warning,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        SectionCard(strings.setLanguage) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = currentLanguage == "es",
                    onClick = { state.settings.update { it.copy(language = "es") } },
                )
                Text("Español", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.width(24.dp))
                RadioButton(
                    selected = currentLanguage == "en",
                    onClick = { state.settings.update { it.copy(language = "en") } },
                )
                Text("English", style = MaterialTheme.typography.bodyMedium)
            }
        }

        PathCard(
            title = strings.setSteamPath,
            help = strings.setSteamFolderHelp,
            path = steamRoot?.toString(),
            valid = steamValid,
            onBrowse = {
                pickFolder(strings.setSteamPath)?.let { picked ->
                    if (state.steamLocator.isValidSteamRoot(picked)) {
                        state.settings.update { it.copy(steamPath = picked.toString()) }
                        message = null
                    } else {
                        message = strings.setInvalidFolder to true
                    }
                }
            },
            onAutoDetect = { state.settings.update { it.copy(steamPath = null) } },
        )

        PathCard(
            title = strings.setArkPath,
            help = strings.setArkFolderHelp,
            path = arkRoot?.toString(),
            valid = arkRoot != null,
            onBrowse = {
                pickFolder(strings.setArkPath)?.let { picked ->
                    if (state.arkLocator.isValidArkRoot(picked)) {
                        state.settings.update { it.copy(arkPath = picked.toString()) }
                        message = null
                    } else {
                        message = strings.setInvalidFolder to true
                    }
                }
            },
            onAutoDetect = { state.settings.update { it.copy(arkPath = null) } },
        )

    }
}

@Composable
private fun PathCard(
    title: String,
    help: String,
    path: String?,
    valid: Boolean,
    onBrowse: () -> Unit,
    onAutoDetect: () -> Unit,
) {
    val strings = LocalStrings.current
    SectionCard(title) {
        Text(
            help,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.size(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                if (valid) Icons.Default.CheckCircle else Icons.Default.Close,
                contentDescription = if (valid) strings.setDetectedOk else strings.setNotFound,
                tint = if (valid) SuccessGreen else MaterialTheme.colorScheme.error,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(10.dp))
            Text(
                path ?: strings.setNotFound,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.size(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onBrowse) { Text(strings.setBrowse) }
            TextButton(onClick = onAutoDetect) { Text(strings.setAutoDetect) }
        }
    }
}

private fun pickFolder(title: String): Path? {
    val chooser = JFileChooser().apply {
        dialogTitle = title
        fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
    }
    return if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
        chooser.selectedFile.toPath()
    } else null
}
