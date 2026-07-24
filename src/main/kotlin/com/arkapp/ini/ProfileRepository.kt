package com.arkapp.ini

import com.arkapp.storage.AppPaths
import com.arkapp.storage.SettingsStore
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.IOException
import java.nio.charset.MalformedInputException
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import kotlin.io.path.listDirectoryEntries
import kotlin.streams.toList

/**
 * Named profiles of the game's BaseDeviceProfiles.ini. Whatever the source
 * (game snapshot, imported file, in-app editor), the stored file is always
 * named BaseDeviceProfiles.ini and apply() copies it into Engine\Config.
 */
class ProfileRepository(
    private val paths: AppPaths,
    private val ark: ArkLocator,
    private val settings: SettingsStore,
) {

    companion object {
        const val FILE_NAME = "BaseDeviceProfiles.ini"

        /** ini files in the wild are usually UTF-8/ASCII, but ANSI ones exist. */
        fun readTextLenient(file: Path): String = try {
            Files.readString(file)
        } catch (_: MalformedInputException) {
            String(Files.readAllBytes(file), StandardCharsets.ISO_8859_1)
        }
    }

    @Serializable
    data class ProfileMeta(val id: String, val name: String, val createdAt: Long)

    enum class ActiveState { ACTIVE, MODIFIED }

    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

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
        val source = gameFile()
        return newProfile(name) { dir -> Files.copy(source, dir.resolve(FILE_NAME)) }
    }

    fun createProfile(name: String, content: String): ProfileMeta =
        newProfile(name) { dir -> Files.writeString(dir.resolve(FILE_NAME), content) }

    fun readContent(profile: ProfileMeta): String =
        readTextLenient(paths.profiles.resolve(profile.id).resolve(FILE_NAME))

    fun updateProfile(profile: ProfileMeta, name: String, content: String) {
        val dir = paths.profiles.resolve(profile.id)
        Files.writeString(dir.resolve(FILE_NAME), content)
        writeMeta(dir, profile.copy(name = name))
    }

    /** Overwrites the game's BaseDeviceProfiles.ini with the profile's copy, backing up first. */
    fun apply(profile: ProfileMeta) {
        val target = gameFile()
        val backupDir = paths.backups.resolve(
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"))
        )
        Files.createDirectories(backupDir)
        Files.copy(target, backupDir.resolve(FILE_NAME), StandardCopyOption.REPLACE_EXISTING)
        Files.copy(
            paths.profiles.resolve(profile.id).resolve(FILE_NAME),
            target,
            StandardCopyOption.REPLACE_EXISTING,
        )
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

    /** State of the profile last applied, comparing the game file against the stored copy. */
    fun activeState(): Pair<ProfileMeta, ActiveState>? {
        val activeId = settings.value.activeProfileId ?: return null
        val meta = list().firstOrNull { it.id == activeId } ?: return null
        val stored = paths.profiles.resolve(activeId).resolve(FILE_NAME)
        val game = runCatching { gameFile() }.getOrNull()
            ?: return meta to ActiveState.MODIFIED
        val identical = Files.isRegularFile(stored) && sha256(game).contentEquals(sha256(stored))
        return meta to (if (identical) ActiveState.ACTIVE else ActiveState.MODIFIED)
    }

    /** Restores the most recent backup created by [apply]. Returns false if none exist. */
    fun restoreLastBackup(): Boolean {
        val latest = runCatching {
            paths.backups.listDirectoryEntries().filter { Files.isDirectory(it) }.maxByOrNull { it.fileName.toString() }
        }.getOrNull() ?: return false
        val source = latest.resolve(FILE_NAME)
        if (!Files.isRegularFile(source)) return false
        Files.copy(source, gameFile(), StandardCopyOption.REPLACE_EXISTING)
        return true
    }

    /** The game's BaseDeviceProfiles.ini; throws if ARK or the file is missing. */
    private fun gameFile(): Path {
        val bdp = ark.baseDeviceProfiles() ?: throw IOException("ARK_NOT_FOUND")
        if (!Files.isRegularFile(bdp)) throw IOException("INI_NOT_FOUND")
        return bdp
    }

    private fun newProfile(name: String, writeFile: (Path) -> Unit): ProfileMeta {
        val id = UUID.randomUUID().toString()
        val dir = paths.profiles.resolve(id)
        Files.createDirectories(dir)
        writeFile(dir)
        val meta = ProfileMeta(id, name, System.currentTimeMillis())
        writeMeta(dir, meta)
        return meta
    }

    private fun writeMeta(dir: Path, meta: ProfileMeta) {
        Files.writeString(dir.resolve("profile.json"), json.encodeToString(ProfileMeta.serializer(), meta))
    }

    private fun sha256(file: Path): ByteArray =
        MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(file))
}
