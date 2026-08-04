package com.arkapp.ini

import com.arkapp.steam.SteamLocator
import com.arkapp.steam.Vdf
import com.arkapp.storage.SettingsStore
import java.nio.file.Files
import java.nio.file.Path

class ArkLocator(private val steam: SteamLocator, private val settings: SettingsStore) {

    companion object {
        const val ARK_APP_ID = "346110"
    }

    fun arkRoot(): Path? {
        settings.value.arkPath?.takeIf { it.isNotBlank() }?.let {
            val p = Path.of(it)
            if (isValidArkRoot(p)) return p
        }
        for (library in steam.libraryRoots()) {
            val steamapps = library.resolve("steamapps")
            val manifest = steamapps.resolve("appmanifest_$ARK_APP_ID.acf")
            if (Files.isRegularFile(manifest)) {
                runCatching {
                    Vdf.parse(Files.readString(manifest)).obj("AppState")?.string("installdir")
                }.getOrNull()?.let { installDir ->
                    val p = steamapps.resolve("common").resolve(installDir)
                    if (isValidArkRoot(p)) return p
                }
            }
            val fallback = steamapps.resolve("common/ARK")
            if (isValidArkRoot(fallback)) return fallback
        }
        return null
    }

    fun isValidArkRoot(p: Path?): Boolean =
        p != null && Files.isDirectory(p.resolve("ShooterGame")) && Files.isDirectory(p.resolve("Engine/Config"))

    fun baseDeviceProfiles(): Path? =
        arkRoot()?.resolve("Engine/Config/BaseDeviceProfiles.ini")

    /** Per-user game config folder (graphics, keybindings…). */
    private fun userConfigDir(): Path? =
        arkRoot()?.resolve("ShooterGame/Saved/Config/WindowsNoEditor")

    fun gameUserSettings(): Path? = userConfigDir()?.resolve("GameUserSettings.ini")

    fun inputIni(): Path? = userConfigDir()?.resolve("Input.ini")
}
