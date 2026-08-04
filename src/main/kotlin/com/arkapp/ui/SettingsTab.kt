package com.arkapp.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    val accent = LocalAccent.current

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        message?.let { (text, isError) -> StatusMessage(text, isError) { message = null } }

        Text(strings.tabSettings, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = Palette.Text)

        if (arkRoot == null) {
            WarnBanner(strings.firstRunBody, Modifier.fillMaxWidth())
        }

        // Idioma
        SettingsRow(title = strings.setLanguage) {
            Surface(
                shape = ButtonShape,
                color = Color.Transparent,
                border = BorderStroke(1.dp, Palette.Border14),
            ) {
                Row {
                    SegmentedOption("Español", currentLanguage == "es") {
                        state.settings.update { it.copy(language = "es") }
                    }
                    Box(Modifier.width(1.dp).height(32.dp)) {
                        Surface(Modifier.fillMaxSize(), color = Palette.Border14) {}
                    }
                    SegmentedOption("English", currentLanguage == "en") {
                        state.settings.update { it.copy(language = "en") }
                    }
                }
            }
        }

        // Color de la app
        SettingsRow(title = strings.setAccent, help = strings.setAccentHelp) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Accents.all.forEach { a ->
                    val selected = a.key == accent.key
                    Surface(
                        onClick = { state.settings.update { it.copy(accentColor = a.key) } },
                        shape = CardShape,
                        color = a.base,
                        border = if (selected) BorderStroke(2.dp, Palette.Text) else null,
                        modifier = Modifier.size(34.dp),
                    ) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            if (selected) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = strings.accentName(a.key),
                                    tint = Palette.OnAccent,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        }
                    }
                }
            }
        }

        // Carpeta de Steam
        PathRow(
            title = strings.setSteamPath,
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

        // Carpeta de ARK
        PathRow(
            title = strings.setArkPath,
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

        // Ventana
        SettingsRow(title = strings.setWindowTitle, help = strings.setWindowHelp) {
            var sizeMenuOpen by remember { mutableStateOf(false) }
            Box {
                GhostButton("${strings.windowSizeName(settings.windowSizePreset)}  ▾") {
                    sizeMenuOpen = true
                }
                androidx.compose.material3.DropdownMenu(
                    expanded = sizeMenuOpen,
                    onDismissRequest = { sizeMenuOpen = false },
                ) {
                    listOf(null, "s", "m", "l", "full").forEach { key ->
                        androidx.compose.material3.DropdownMenuItem(
                            text = { Text(strings.windowSizeName(key), fontSize = 13.sp) },
                            leadingIcon = {
                                if (key == settings.windowSizePreset) {
                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                }
                            },
                            onClick = {
                                state.settings.update {
                                    it.copy(
                                        windowSizePreset = key,
                                        windowMaximized = if (key != null) false else it.windowMaximized,
                                    )
                                }
                                sizeMenuOpen = false
                            },
                        )
                    }
                }
            }
            GhostButton(strings.setWindowReset) {
                state.settings.update {
                    it.copy(
                        windowWidth = null,
                        windowHeight = null,
                        windowMaximized = false,
                        windowSizePreset = null,
                    )
                }
            }
        }

        // Puesta a punto
        SettingsRow(title = strings.setupTitle, help = strings.setShowSetupAgainHelp) {
            GhostButton(strings.setShowSetupAgain) {
                state.settings.update { it.copy(setupDismissed = false) }
            }
        }
    }
}

/** Horizontal settings card: title (+ optional help) on the left, controls on the right. */
@Composable
private fun SettingsRow(
    title: String,
    help: String? = null,
    content: @Composable () -> Unit,
) {
    AppCard(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Palette.Text)
                if (help != null) {
                    Spacer(Modifier.height(4.dp))
                    Text(help, fontSize = 12.5.sp, color = Palette.Muted, lineHeight = 18.sp)
                }
            }
            content()
        }
    }
}

@Composable
private fun SegmentedOption(label: String, selected: Boolean, onClick: () -> Unit) {
    val accent = LocalAccent.current
    Surface(
        onClick = onClick,
        color = Color.Transparent,
        border = if (selected) BorderStroke(1.dp, accent.base) else null,
    ) {
        Text(
            label,
            fontSize = 12.5.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) accent.hi else Palette.Muted,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
        )
    }
}

@Composable
private fun PathRow(
    title: String,
    path: String?,
    valid: Boolean,
    onBrowse: () -> Unit,
    onAutoDetect: () -> Unit,
) {
    val strings = LocalStrings.current
    AppCard(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Palette.Text)
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        path ?: strings.setNotFound,
                        fontSize = 12.sp,
                        color = Palette.Muted,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    Text(
                        if (valid) "✓ ${strings.setValid}" else "✕ ${strings.setNotFound}",
                        fontSize = 11.sp,
                        color = if (valid) Palette.Success else Palette.Danger,
                    )
                }
            }
            GhostButton(strings.setBrowse, onClick = onBrowse)
            GhostButton(strings.setAutoDetect, onClick = onAutoDetect)
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
