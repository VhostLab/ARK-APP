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
    val profiles = ProfileRepository(paths, arkLocator, settings)

    // First-run checklist progress, derived from real state. Null until first check.
    // The tabs push updates after mutations; refreshSetupState() reads from disk (call on IO).
    val setupFavoritesDone = MutableStateFlow<Boolean?>(null)
    val setupProfileDone = MutableStateFlow<Boolean?>(null)

    fun refreshSetupState() {
        setupFavoritesDone.value = runCatching { favorites.list().isNotEmpty() }.getOrDefault(false)
        setupProfileDone.value = runCatching { profiles.activeState() != null }.getOrDefault(false)
    }
}
