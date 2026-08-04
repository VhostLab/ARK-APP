package com.arkapp

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.window.Tray
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.arkapp.i18n.EnStrings
import com.arkapp.i18n.EsStrings
import com.arkapp.ui.App
import com.arkapp.ui.systemLanguage
import java.awt.Dimension
import java.awt.Toolkit
import javax.swing.UIManager
import kotlinx.coroutines.flow.debounce

fun main() {
    // Swing dialogs (folder pickers) follow the Windows look instead of Metal
    runCatching { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()) }
    runApp()
}

private fun runApp() = application {
    val state = remember { AppState() }
    val settingsState by state.settings.state.collectAsState()
    val strings = if ((settingsState.language ?: systemLanguage()) == "es") EsStrings else EnStrings
    // Closing the window hides the app to the system tray; "Salir" really exits.
    var windowVisible by remember { mutableStateOf(true) }
    Tray(
        icon = painterResource("icon.png"),
        tooltip = "Prodigiosos App",
        onAction = { windowVisible = true },
        menu = {
            Item(strings.trayOpen, onClick = { windowVisible = true })
            Item(strings.trayExit, onClick = ::exitApplication)
        },
    )
    // Window size: a fixed preset chosen in Settings always wins; otherwise the
    // window remembers how the user left it (size + maximized). Default is a
    // near-fullscreen FLOATING window, not maximized: VRR/G-Sync monitors treat
    // maximized windows as fullscreen and judder on the app's irregular present
    // pacing; floating stays composited by DWM.
    val saved = state.settings.value
    val screen = Toolkit.getDefaultToolkit().screenSize
    fun presetSize(key: String?): DpSize? = when (key) {
        "s" -> DpSize(1280.dp, 800.dp)
        "m" -> DpSize(1600.dp, 950.dp)
        "l" -> DpSize(1920.dp, 1080.dp)
        "full" -> DpSize(
            maxOf(1280, (screen.width * 0.92).toInt()).dp,
            maxOf(800, (screen.height * 0.90).toInt()).dp,
        )
        else -> null
    }
    val initialSize = presetSize(saved.windowSizePreset)
        ?: if (saved.windowWidth != null && saved.windowHeight != null) {
            DpSize(saved.windowWidth.dp, saved.windowHeight.dp)
        } else {
            presetSize("full")!!
        }
    val windowState = rememberWindowState(
        placement = if (saved.windowMaximized && saved.windowSizePreset == null) {
            WindowPlacement.Maximized
        } else {
            WindowPlacement.Floating
        },
        position = WindowPosition(Alignment.Center),
        size = initialSize,
    )
    // Live-apply a preset picked in Settings.
    LaunchedEffect(settingsState.windowSizePreset) {
        presetSize(settingsState.windowSizePreset)?.let { size ->
            windowState.placement = WindowPlacement.Floating
            windowState.size = size
            windowState.position = WindowPosition(Alignment.Center)
        }
    }
    Window(
        onCloseRequest = { windowVisible = false },
        visible = windowVisible,
        title = "Prodigiosos App",
        icon = painterResource("icon.png"),
        state = windowState,
    ) {
        LaunchedEffect(Unit) {
            window.minimumSize = Dimension(960, 640)
            if (System.getenv("ARKAPP_FPS") != null) println("ARKAPP_RENDER: ${window.renderApi}")
        }
        LaunchedEffect(Unit) {
            snapshotFlow { windowState.size to windowState.placement }
                .debounce(400)
                .collect { (size, placement) ->
                    // A fixed preset always wins: session resizes are not remembered.
                    if (state.settings.value.windowSizePreset != null) return@collect
                    when {
                        placement == WindowPlacement.Maximized ->
                            state.settings.update { it.copy(windowMaximized = true) }
                        placement == WindowPlacement.Floating && size.isSpecified ->
                            state.settings.update {
                                it.copy(
                                    windowWidth = size.width.value.toInt(),
                                    windowHeight = size.height.value.toInt(),
                                    windowMaximized = false,
                                )
                            }
                    }
                }
        }
        App(state)
    }
}
