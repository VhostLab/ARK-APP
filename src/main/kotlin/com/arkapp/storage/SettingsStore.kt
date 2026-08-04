package com.arkapp.storage

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

@Serializable
data class AppSettings(
    val language: String? = null,        // "es" | "en" | null = system language
    val steamPath: String? = null,
    val arkPath: String? = null,
    val steamAccountId: String? = null,
    val activeProfileId: String? = null,
    val activeGusProfileId: String? = null,
    val activeInputProfileId: String? = null,
    val setupDismissed: Boolean = false,
    val accentColor: String? = null,   // Accents key; null = default (morado)
    val pinnedServers: List<String> = emptyList(),   // favorite addresses pinned to the top (lowercase)
    val defaultInisVersion: Int = 0,   // last bundled-profiles set seeded into the profile store
    val windowWidth: Int? = null,
    val windowHeight: Int? = null,
    val windowMaximized: Boolean = false,
    val windowSizePreset: String? = null,   // null = remember last; "s"|"m"|"l"|"full" = fixed size
)

class SettingsStore(private val file: Path) {
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

    private val _state = MutableStateFlow(load())
    val state: StateFlow<AppSettings> = _state
    val value: AppSettings get() = _state.value

    private fun load(): AppSettings = runCatching {
        if (Files.isRegularFile(file)) json.decodeFromString<AppSettings>(Files.readString(file)) else AppSettings()
    }.getOrDefault(AppSettings())

    @Synchronized
    fun update(transform: (AppSettings) -> AppSettings) {
        val next = transform(_state.value)
        if (next == _state.value) return
        _state.value = next
        Files.createDirectories(file.parent)
        val tmp = file.resolveSibling(file.fileName.toString() + ".tmp")
        Files.writeString(tmp, json.encodeToString(AppSettings.serializer(), next))
        Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
    }
}
