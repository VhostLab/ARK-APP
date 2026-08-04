package com.arkapp.ini

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
 * Named profiles of one of the game's ini files (BaseDeviceProfiles,
 * GameUserSettings, Input…). Whatever the source (game snapshot, imported
 * file, in-app editor), the stored copy always uses the game's file name and
 * apply() copies it over the game's file after backing the original up.
 */
class ProfileRepository(
    private val profilesDir: Path,
    private val backupsDir: Path,
    private val fileName: String,
    private val gameFileProvider: () -> Path?,
    private val activeId: () -> String?,
    private val setActiveId: (String?) -> Unit,
) {

    companion object {
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
        profilesDir.listDirectoryEntries()
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
        return newProfile(name) { dir -> Files.copy(source, dir.resolve(fileName)) }
    }

    fun createProfile(name: String, content: String): ProfileMeta =
        newProfile(name) { dir -> Files.writeString(dir.resolve(fileName), content) }

    /** Byte-exact copy of any external file into the app's folder; the source is never touched. */
    fun importFile(name: String, source: Path): ProfileMeta =
        newProfile(name) { dir -> Files.copy(source, dir.resolve(fileName)) }

    fun readContent(profile: ProfileMeta): String =
        readTextLenient(profilesDir.resolve(profile.id).resolve(fileName))

    fun updateProfile(profile: ProfileMeta, name: String, content: String) {
        val dir = profilesDir.resolve(profile.id)
        Files.writeString(dir.resolve(fileName), content)
        writeMeta(dir, profile.copy(name = name))
    }

    /** Overwrites the game's target ini with the profile's copy, backing up first. */
    fun apply(profile: ProfileMeta) {
        val target = gameFile()
        val backupDir = backupsDir.resolve(
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"))
        )
        Files.createDirectories(backupDir)
        Files.copy(target, backupDir.resolve(fileName), StandardCopyOption.REPLACE_EXISTING)
        Files.copy(
            profilesDir.resolve(profile.id).resolve(fileName),
            target,
            StandardCopyOption.REPLACE_EXISTING,
        )
        setActiveId(profile.id)
    }

    fun delete(profile: ProfileMeta) {
        val dir = profilesDir.resolve(profile.id)
        if (Files.isDirectory(dir)) {
            Files.walk(dir).use { stream ->
                stream.sorted(Comparator.reverseOrder()).toList().forEach(Files::deleteIfExists)
            }
        }
        if (activeId() == profile.id) {
            setActiveId(null)
        }
    }

    /** State of the profile last applied, comparing the game file against the stored copy. */
    fun activeState(): Pair<ProfileMeta, ActiveState>? {
        val activeId = activeId() ?: return null
        val meta = list().firstOrNull { it.id == activeId } ?: return null
        val stored = profilesDir.resolve(activeId).resolve(fileName)
        val game = runCatching { gameFile() }.getOrNull()
            ?: return meta to ActiveState.MODIFIED
        val identical = Files.isRegularFile(stored) && sha256(game).contentEquals(sha256(stored))
        return meta to (if (identical) ActiveState.ACTIVE else ActiveState.MODIFIED)
    }

    /** Restores the most recent backup created by [apply]. Returns false if none exist. */
    fun restoreLastBackup(): Boolean {
        val latest = runCatching {
            backupsDir.listDirectoryEntries().filter { Files.isDirectory(it) }.maxByOrNull { it.fileName.toString() }
        }.getOrNull() ?: return false
        val source = latest.resolve(fileName)
        if (!Files.isRegularFile(source)) return false
        Files.copy(source, gameFile(), StandardCopyOption.REPLACE_EXISTING)
        return true
    }

    /** The game's target ini; throws if ARK or the file is missing. */
    private fun gameFile(): Path {
        val target = gameFileProvider() ?: throw IOException("ARK_NOT_FOUND")
        if (!Files.isRegularFile(target)) throw IOException("INI_NOT_FOUND")
        return target
    }

    private fun newProfile(name: String, writeFile: (Path) -> Unit): ProfileMeta {
        val id = UUID.randomUUID().toString()
        val dir = profilesDir.resolve(id)
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
