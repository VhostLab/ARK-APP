package com.arkapp.steam

import kotlinx.coroutines.delay
import java.nio.file.Files
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

    /** Reopens Steam after the app closed it to write favorites. */
    fun launchSteam(steamRoot: Path) {
        runCatching { ProcessBuilder(steamRoot.resolve("steam.exe").toString()).start() }
    }

    /**
     * Connects to a server via steam://connect (address uses the query port).
     * Steam starts itself if it is not running and launches ARK joining the server.
     */
    fun launchConnect(steamRoot: Path?, address: String): Boolean = runCatching {
        val uri = "steam://connect/$address"
        val exe = steamRoot?.resolve("steam.exe")
        if (exe != null && Files.isRegularFile(exe)) {
            ProcessBuilder(exe.toString(), uri).start()
        } else {
            ProcessBuilder("cmd", "/c", "start", "", uri).start()
        }
        true
    }.getOrDefault(false)

    suspend fun awaitSteamExit(timeout: Duration = 30.seconds): Boolean {
        val deadline = System.nanoTime() + timeout.inWholeNanoseconds
        while (System.nanoTime() < deadline) {
            if (!isSteamRunning()) return true
            delay(500)
        }
        return !isSteamRunning()
    }
}
