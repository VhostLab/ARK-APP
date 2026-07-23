package com.arkapp.steam

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

/**
 * Reads and writes Steam's serverbrowser_hist.vdf. The caller is responsible for
 * making sure Steam is closed before any mutating call (Steam rewrites the file
 * on exit, discarding external edits).
 */
class FavoritesRepository(private val fileProvider: () -> Path?) {

    companion object {
        const val ARK_APP_ID = "346110"
    }

    data class Favorite(val entryKey: String, val name: String, val address: String, val appId: String)

    data class NewFavorite(val name: String, val address: String)

    fun list(): List<Favorite> {
        val file = fileProvider() ?: return emptyList()
        if (!Files.isRegularFile(file)) return emptyList()
        val favs = Vdf.parse(Files.readString(file)).obj("Filters")?.obj("Favorites") ?: return emptyList()
        return favs.entries.mapNotNull { (key, v) ->
            (v as? VdfObject)?.let {
                Favorite(key, it.string("name").orEmpty(), it.string("address").orEmpty(), it.string("appid") ?: "0")
            }
        }
    }

    /** Returns (added, skippedAsDuplicate). */
    fun add(servers: List<NewFavorite>): Pair<Int, Int> {
        val file = fileProvider() ?: throw IllegalStateException("Steam account not resolved")
        val root = if (Files.isRegularFile(file)) Vdf.parse(Files.readString(file)) else VdfObject()
        val filters = root.getOrCreateObj("Filters")
        val favs = filters.getOrCreateObj("Favorites")
        filters.getOrCreateObj("History")

        val existing = favs.entries.mapNotNullTo(HashSet()) {
            (it.second as? VdfObject)?.string("address")?.lowercase()
        }
        var next = (favs.entries.mapNotNull { it.first.toIntOrNull() }.maxOrNull() ?: 0) + 1
        var added = 0
        for (server in servers) {
            if (!existing.add(server.address.lowercase())) continue
            favs.entries += next.toString() to VdfObject(
                mutableListOf(
                    "name" to VdfString(server.name.ifBlank { server.address }),
                    "address" to VdfString(server.address),
                    "LastPlayed" to VdfString("0"),
                    "appid" to VdfString(ARK_APP_ID),
                    "accountid" to VdfString("0"),
                )
            )
            next++
            added++
        }
        if (added > 0) save(file, root)
        return added to (servers.size - added)
    }

    fun remove(entryKeys: Set<String>): Int {
        val file = fileProvider() ?: return 0
        if (!Files.isRegularFile(file)) return 0
        val root = Vdf.parse(Files.readString(file))
        val favs = root.obj("Filters")?.obj("Favorites") ?: return 0
        val before = favs.entries.size
        favs.entries.removeAll { it.first in entryKeys }
        val removed = before - favs.entries.size
        if (removed > 0) save(file, root)
        return removed
    }

    private fun save(file: Path, root: VdfObject) {
        Files.createDirectories(file.parent)
        if (Files.isRegularFile(file)) {
            Files.copy(
                file, file.resolveSibling(file.fileName.toString() + ".arkapp-bak"),
                StandardCopyOption.REPLACE_EXISTING,
            )
        }
        val tmp = file.resolveSibling(file.fileName.toString() + ".tmp")
        Files.writeString(tmp, Vdf.write(root))
        Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
    }
}
