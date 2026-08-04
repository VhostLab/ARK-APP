package com.arkapp

import com.arkapp.ini.ArkLocator
import com.arkapp.ini.ProfileRepository
import com.arkapp.steam.FavoritesRepository
import com.arkapp.steam.SteamLocator
import com.arkapp.storage.AppPaths
import com.arkapp.storage.SettingsStore
import kotlinx.coroutines.flow.MutableStateFlow

class AppState {
    val paths = AppPaths()
    val settings = SettingsStore(paths.settingsFile)
    val steamLocator = SteamLocator(settings)
    val arkLocator = ArkLocator(steamLocator, settings)
    val favorites = FavoritesRepository { steamLocator.favoritesFile() }
    val profiles = ProfileRepository(
        profilesDir = paths.profiles,
        backupsDir = paths.backups,
        fileName = "BaseDeviceProfiles.ini",
        gameFileProvider = { arkLocator.baseDeviceProfiles() },
        activeId = { settings.value.activeProfileId },
        setActiveId = { id -> settings.update { it.copy(activeProfileId = id) } },
    )
    val gusProfiles = ProfileRepository(
        profilesDir = paths.gusProfiles,
        backupsDir = paths.gusBackups,
        fileName = "GameUserSettings.ini",
        gameFileProvider = { arkLocator.gameUserSettings() },
        activeId = { settings.value.activeGusProfileId },
        setActiveId = { id -> settings.update { it.copy(activeGusProfileId = id) } },
    )
    val inputProfiles = ProfileRepository(
        profilesDir = paths.inputProfiles,
        backupsDir = paths.inputBackups,
        fileName = "Input.ini",
        gameFileProvider = { arkLocator.inputIni() },
        activeId = { settings.value.activeInputProfileId },
        setActiveId = { id -> settings.update { it.copy(activeInputProfileId = id) } },
    )

    // First-run checklist progress, derived from real state. Null until first check.
    // The tabs push updates after mutations; refreshSetupState() reads from disk (call on IO).
    val setupFavoritesDone = MutableStateFlow<Boolean?>(null)
    val setupProfileDone = MutableStateFlow<Boolean?>(null)

    fun refreshSetupState() {
        setupFavoritesDone.value = runCatching { favorites.list().isNotEmpty() }.getOrDefault(false)
        setupProfileDone.value = runCatching { profiles.activeState() != null }.getOrDefault(false)
    }

    /** Community INIs bundled with the app, created once per install (bump the version to ship more). */
    fun seedDefaultProfiles() {
        val seededVersion = 1
        if (settings.value.defaultInisVersion >= seededVersion) return
        val defaults = listOf(
            "App INI - farm normal" to "/defaultinis/farm-normal.ini",
            "App INI - HARD" to "/defaultinis/hard.ini",
            "App INI - super hard (no estructuras, no disparados tek ni debuffs etc...)" to "/defaultinis/super-hard.ini",
        )
        runCatching {
            val existing = profiles.list().map { it.name }.toSet()
            for ((name, resource) in defaults) {
                if (name in existing) continue
                val bytes = javaClass.getResourceAsStream(resource)?.use { it.readBytes() } ?: continue
                val content = runCatching { String(bytes, Charsets.UTF_8) }
                    .getOrElse { String(bytes, Charsets.ISO_8859_1) }
                profiles.createProfile(name, content)
            }
        }
        settings.update { it.copy(defaultInisVersion = seededVersion) }
    }
}
