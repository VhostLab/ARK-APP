package com.arkapp.storage

import java.nio.file.Files
import java.nio.file.Path

class AppPaths {
    val root: Path = Path.of(System.getenv("APPDATA") ?: System.getProperty("user.home"), "ARK-APP")
    val profiles: Path = root.resolve("profiles")
    val backups: Path = root.resolve("backups")
    val gusProfiles: Path = root.resolve("profiles-gus")
    val gusBackups: Path = root.resolve("backups-gus")
    val inputProfiles: Path = root.resolve("profiles-input")
    val inputBackups: Path = root.resolve("backups-input")
    val settingsFile: Path = root.resolve("settings.json")

    init {
        Files.createDirectories(profiles)
        Files.createDirectories(backups)
        Files.createDirectories(gusProfiles)
        Files.createDirectories(gusBackups)
        Files.createDirectories(inputProfiles)
        Files.createDirectories(inputBackups)
    }
}
