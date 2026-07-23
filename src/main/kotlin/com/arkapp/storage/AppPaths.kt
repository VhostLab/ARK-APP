package com.arkapp.storage

import java.nio.file.Files
import java.nio.file.Path

class AppPaths {
    val root: Path = Path.of(System.getenv("APPDATA") ?: System.getProperty("user.home"), "ARK-APP")
    val profiles: Path = root.resolve("profiles")
    val backups: Path = root.resolve("backups")
    val settingsFile: Path = root.resolve("settings.json")

    init {
        Files.createDirectories(profiles)
        Files.createDirectories(backups)
    }
}
