package com.arkapp.steam

import kotlinx.coroutines.delay
import java.nio.file.Path
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

object SteamProcess {

    fun isSteamRunning(): Boolean = isProcessRunning("\\steam.exe")

    fun isArkRunning(): Boolean = isProcessRunning("\\ShooterGame.exe")

    private fun isProcessRunning(commandSuffix: String): Boolean =
        ProcessHandle.allProcesses().anyMatch {
            it.info().command().orElse("").endsWith(commandSuffix, ignoreCase = true)
        }

    /** Asks Steam to shut down gracefully (official -shutdown flag). */
    fun requestSteamShutdown(steamRoot: Path) {
        ProcessBuilder(steamRoot.resolve("steam.exe").toString(), "-shutdown").start()
    }

    suspend fun awaitSteamExit(timeout: Duration = 30.seconds): Boolean {
        val deadline = System.nanoTime() + timeout.inWholeNanoseconds
        while (System.nanoTime() < deadline) {
            if (!isSteamRunning()) return true
            delay(500)
        }
        return !isSteamRunning()
    }
}
