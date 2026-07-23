package com.arkapp

import androidx.compose.runtime.remember
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.arkapp.ui.App

fun main() = application {
    val state = remember { AppState() }
    Window(
        onCloseRequest = ::exitApplication,
        title = "ARK-APP",
        icon = painterResource("icon.png"),
        state = rememberWindowState(width = 1000.dp, height = 700.dp),
    ) {
        App(state)
    }
}
