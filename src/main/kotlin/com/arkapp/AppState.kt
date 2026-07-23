package com.arkapp

import com.arkapp.ini.ArkLocator
import com.arkapp.ini.ProfileRepository
import com.arkapp.steam.FavoritesRepository
import com.arkapp.steam.SteamLocator
import com.arkapp.storage.AppPaths
import com.arkapp.storage.SettingsStore

class AppState {
    val paths = AppPaths()
    val settings = SettingsStore(paths.settingsFile)
    val steamLocator = SteamLocator(settings)
    val arkLocator = ArkLocator(steamLocator, settings)
    val favorites = FavoritesRepository { steamLocator.favoritesFile() }
    val profiles = ProfileRepository(paths, arkLocator, settings)
}
