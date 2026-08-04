package com.arkapp

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.arkapp.ui.App
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
    // The window remembers how the user left it (size + maximized) and reopens
    // the same way. Default is a near-fullscreen FLOATING window, not maximized:
    // VRR/G-Sync monitors treat maximized windows as fullscreen and judder on
    // the app's irregular present pacing; floating stays composited by DWM.
    val saved = state.settings.value
    val screen = Toolkit.getDefaultToolkit().screenSize
    val initialSize =
        if (saved.windowWidth != null && saved.windowHeight != null) {
            DpSize(saved.windowWidth.dp, saved.windowHeight.dp)
        } else {
            DpSize(
                maxOf(1280, (screen.width * 0.92).toInt()).dp,
                maxOf(800, (screen.height * 0.90).toInt()).dp,
            )
        }
    val windowState = rememberWindowState(
        placement = if (saved.windowMaximized) WindowPlacement.Maximized else WindowPlacement.Floating,
        position = WindowPosition(Alignment.Center),
        size = initialSize,
    )
    Window(
        onCloseRequest = ::exitApplication,
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
