package com.arkapp.steam

import com.arkapp.storage.SettingsStore
import java.nio.file.Files
import java.nio.file.Path

class SteamLocator(private val settings: SettingsStore) {

    companion object {
        const val STEAM64_BASE = 76561197960265728L
    }

    fun steamRoot(): Path? {
        settings.value.steamPath?.takeIf { it.isNotBlank() }?.let {
            val p = Path.of(it)
            if (Files.isDirectory(p)) return p
        }
        val candidates = listOfNotNull(registrySteamPath(), "C:/Program Files (x86)/Steam")
        return candidates.map { Path.of(it) }.firstOrNull { Files.isRegularFile(it.resolve("steam.exe")) }
    }

    fun isValidSteamRoot(p: Path?): Boolean = p != null && Files.isRegularFile(p.resolve("steam.exe"))

    private fun registrySteamPath(): String? = runCatching {
        val out = ProcessBuilder("reg", "query", "HKCU\\Software\\Valve\\Steam", "/v", "SteamPath")
            .redirectErrorStream(true).start().inputStream.bufferedReader().readText()
        Regex("SteamPath\\s+REG_SZ\\s+(.+)").find(out)?.groupValues?.get(1)?.trim()
    }.getOrNull()

    /** 32-bit accountid of the most recently used Steam account. */
    fun accountId(): String? {
        settings.value.steamAccountId?.takeIf { it.isNotBlank() }?.let { return it }
        val root = steamRoot() ?: return null

        val loginUsers = root.resolve("config/loginusers.vdf")
        if (Files.isRegularFile(loginUsers)) {
            runCatching {
                val users = Vdf.parse(Files.readString(loginUsers)).obj("users")
                val entry = users?.entries?.firstOrNull { (_, v) ->
                    (v as? VdfObject)?.string("MostRecent") == "1"
                } ?: users?.entries?.firstOrNull()
                entry?.first?.toLongOrNull()?.let { return (it - STEAM64_BASE).toString() }
            }
        }

        // Fallback: most recently modified numeric folder under userdata
        val userdata = root.resolve("userdata")
        if (!Files.isDirectory(userdata)) return null
        return runCatching {
            Files.list(userdata).use { stream ->
                stream.filter { Files.isDirectory(it) && it.fileName.toString().toLongOrNull() != null }
                    .max(Comparator.comparingLong { Files.getLastModifiedTime(it).toMillis() })
                    .map { it.fileName.toString() }
                    .orElse(null)
            }
        }.getOrNull()
    }

    fun availableAccountIds(): List<String> {
        val userdata = steamRoot()?.resolve("userdata") ?: return emptyList()
        if (!Files.isDirectory(userdata)) return emptyList()
        return runCatching {
            Files.list(userdata).use { stream ->
                stream.filter { Files.isDirectory(it) && it.fileName.toString().toLongOrNull() != null }
                    .map { it.fileName.toString() }.toList()
            }
        }.getOrDefault(emptyList())
    }

    /** Modern location of the server browser favorites/history file. */
    fun favoritesFile(): Path? {
        val root = steamRoot() ?: return null
        val account = accountId() ?: return null
        return root.resolve("userdata").resolve(account).resolve("7/remote/serverbrowser_hist.vdf")
    }

    fun libraryRoots(): List<Path> {
        val root = steamRoot() ?: return emptyList()
        val result = linkedSetOf(root)
        val lf = root.resolve("steamapps/libraryfolders.vdf")
        if (Files.isRegularFile(lf)) {
            runCatching {
                val parsed = Vdf.parse(Files.readString(lf))
                val libs = parsed.obj("libraryfolders")
                libs?.entries?.forEach { (_, v) ->
                    (v as? VdfObject)?.string("path")?.let { result.add(Path.of(it)) }
                }
            }
        }
        return result.filter { Files.isDirectory(it) }
    }
}
