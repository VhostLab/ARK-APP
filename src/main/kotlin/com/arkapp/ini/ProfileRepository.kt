package com.arkapp.ini

import com.arkapp.storage.AppPaths
import com.arkapp.storage.SettingsStore
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import kotlin.io.path.listDirectoryEntries
import kotlin.streams.toList

class ProfileRepository(
    private val paths: AppPaths,
    private val ark: ArkLocator,
    private val settings: SettingsStore,
) {

    @Serializable
    data class ProfileMeta(val id: String, val name: String, val createdAt: Long)

    enum class ActiveState { ACTIVE, MODIFIED }

    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }
    private val fileNames = listOf("GameUserSettings.ini", "BaseDeviceProfiles.ini")

    fun list(): List<ProfileMeta> = runCatching {
        paths.profiles.listDirectoryEntries()
            .filter { Files.isDirectory(it) }
            .mapNotNull { dir ->
                runCatching {
                    json.decodeFromString(ProfileMeta.serializer(), Files.readString(dir.resolve("profile.json")))
                }.getOrNull()
            }
            .sortedBy { it.createdAt }
    }.getOrDefault(emptyList())

    fun saveCurrentAs(name: String): ProfileMeta {
        val sources = gameFiles()
        val id = UUID.randomUUID().toString()
        val dir = paths.profiles.resolve(id)
        Files.createDirectories(dir)
        sources.forEach { Files.copy(it, dir.resolve(it.fileName.toString())) }
        val meta = ProfileMeta(id, name, System.currentTimeMillis())
        Files.writeString(dir.resolve("profile.json"), json.encodeToString(ProfileMeta.serializer(), meta))
        return meta
    }

    /** Overwrites the game's ini files with the profile's copies, backing up first. */
    fun apply(profile: ProfileMeta) {
        val targets = gameFiles()
        val profileDir = paths.profiles.resolve(profile.id)

        val backupDir = paths.backups.resolve(
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"))
        )
        Files.createDirectories(backupDir)
        targets.forEach { Files.copy(it, backupDir.resolve(it.fileName.toString()), StandardCopyOption.REPLACE_EXISTING) }

        targets.forEach { target ->
            Files.copy(profileDir.resolve(target.fileName.toString()), target, StandardCopyOption.REPLACE_EXISTING)
        }
        settings.update { it.copy(activeProfileId = profile.id) }
    }

    fun delete(profile: ProfileMeta) {
        val dir = paths.profiles.resolve(profile.id)
        if (Files.isDirectory(dir)) {
            Files.walk(dir).use { stream ->
                stream.sorted(Comparator.reverseOrder()).toList().forEach(Files::deleteIfExists)
            }
        }
        if (settings.value.activeProfileId == profile.id) {
            settings.update { it.copy(activeProfileId = null) }
        }
    }

    /** State of the profile last applied, comparing game files against stored copies. */
    fun activeState(): Pair<ProfileMeta, ActiveState>? {
        val activeId = settings.value.activeProfileId ?: return null
        val meta = list().firstOrNull { it.id == activeId } ?: return null
        val profileDir = paths.profiles.resolve(activeId)
        val targets = runCatching { gameFiles() }.getOrNull() ?: return meta to ActiveState.MODIFIED
        val identical = fileNames.all { name ->
            val game = targets.firstOrNull { it.fileName.toString() == name } ?: return@all false
            val stored = profileDir.resolve(name)
            Files.isRegularFile(stored) && sha256(game).contentEquals(sha256(stored))
        }
        return meta to (if (identical) ActiveState.ACTIVE else ActiveState.MODIFIED)
    }

    /** Restores the most recent backup created by [apply]. Returns false if none exist. */
    fun restoreLastBackup(): Boolean {
        val latest = runCatching {
            paths.backups.listDirectoryEntries().filter { Files.isDirectory(it) }.maxByOrNull { it.fileName.toString() }
        }.getOrNull() ?: return false
        val targets = gameFiles()
        targets.forEach { target ->
            val source = latest.resolve(target.fileName.toString())
            if (Files.isRegularFile(source)) {
                Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING)
            }
        }
        return true
    }

    /** The game's two ini files; throws if ARK or the files are missing. */
    private fun gameFiles(): List<Path> {
        val gus = ark.gameUserSettings() ?: throw IOException("ARK_NOT_FOUND")
        val bdp = ark.baseDeviceProfiles() ?: throw IOException("ARK_NOT_FOUND")
        if (!Files.isRegularFile(gus) || !Files.isRegularFile(bdp)) throw IOException("INI_NOT_FOUND")
        return listOf(gus, bdp)
    }

    private fun sha256(file: Path): ByteArray =
        MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(file))
}
