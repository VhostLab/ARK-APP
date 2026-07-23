package com.arkapp

import com.arkapp.steam.FavoritesRepository
import com.arkapp.steam.Vdf
import com.arkapp.steam.VdfObject
import com.arkapp.steam.VdfString
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class FavoritesMergeTest {

    @TempDir
    lateinit var tempDir: Path

    private fun file(): Path = tempDir.resolve("serverbrowser_hist.vdf")

    private fun repo() = FavoritesRepository { file() }

    private fun favoriteEntry(address: String, appId: String) = VdfObject(
        mutableListOf(
            "name" to VdfString(address),
            "address" to VdfString(address),
            "LastPlayed" to VdfString("0"),
            "appid" to VdfString(appId),
            "accountid" to VdfString("0"),
        )
    )

    private fun writeInitialFile() {
        val root = VdfObject()
        val filters = root.getOrCreateObj("Filters")
        val favs = filters.getOrCreateObj("Favorites")
        favs.entries += "1" to favoriteEntry("65.21.137.238:27015", "0")
        favs.entries += "5" to favoriteEntry("10.10.10.10:27016", "730")
        val history = filters.getOrCreateObj("History")
        history.entries += "1" to favoriteEntry("65.21.137.238:27015", "346110")
        Files.writeString(file(), Vdf.write(root))
    }

    @Test
    fun `add merges with dedupe and continues numbering`() {
        writeInitialFile()
        val (added, skipped) = repo().add(
            listOf(
                FavoritesRepository.NewFavorite("Nuevo", "20.20.20.20:27015"),
                FavoritesRepository.NewFavorite("Dup", "65.21.137.238:27015"),
                FavoritesRepository.NewFavorite("Otro", "30.30.30.30:27017"),
            )
        )
        assertEquals(2, added)
        assertEquals(1, skipped)

        val favs = Vdf.parse(Files.readString(file())).obj("Filters")!!.obj("Favorites")!!
        assertEquals(4, favs.entries.size)
        // Numbering continues after the highest existing key (5)
        assertEquals(listOf("1", "5", "6", "7"), favs.entries.map { it.first })
        val newEntry = favs.entries[2].second as VdfObject
        assertEquals("Nuevo", newEntry.string("name"))
        assertEquals("20.20.20.20:27015", newEntry.string("address"))
        assertEquals(FavoritesRepository.ARK_APP_ID, newEntry.string("appid"))
    }

    @Test
    fun `add preserves history and other games' favorites`() {
        writeInitialFile()
        repo().add(listOf(FavoritesRepository.NewFavorite("x", "20.20.20.20:27015")))

        val root = Vdf.parse(Files.readString(file()))
        val history = root.obj("Filters")!!.obj("History")!!
        assertEquals(1, history.entries.size)
        assertEquals("65.21.137.238:27015", (history.entries[0].second as VdfObject).string("address"))

        val otherGame = root.obj("Filters")!!.obj("Favorites")!!.entries
            .first { it.first == "5" }.second as VdfObject
        assertEquals("730", otherGame.string("appid"))
    }

    @Test
    fun `add creates file with full structure when missing`() {
        val (added, _) = repo().add(listOf(FavoritesRepository.NewFavorite("x", "1.2.3.4:27015")))
        assertEquals(1, added)
        val root = Vdf.parse(Files.readString(file()))
        assertNotNull(root.obj("Filters")!!.obj("Favorites"))
        assertNotNull(root.obj("Filters")!!.obj("History"))
        assertEquals("1", root.obj("Filters")!!.obj("Favorites")!!.entries[0].first)
    }

    @Test
    fun `add makes a backup of the existing file`() {
        writeInitialFile()
        repo().add(listOf(FavoritesRepository.NewFavorite("x", "20.20.20.20:27015")))
        assertTrue(Files.isRegularFile(tempDir.resolve("serverbrowser_hist.vdf.arkapp-bak")))
    }

    @Test
    fun `remove deletes only the given keys`() {
        writeInitialFile()
        val removed = repo().remove(setOf("1"))
        assertEquals(1, removed)
        val favs = Vdf.parse(Files.readString(file())).obj("Filters")!!.obj("Favorites")!!
        assertEquals(1, favs.entries.size)
        assertEquals("5", favs.entries[0].first)
    }

    @Test
    fun `list returns favorites with entry keys`() {
        writeInitialFile()
        val list = repo().list()
        assertEquals(2, list.size)
        assertEquals("1", list[0].entryKey)
        assertEquals("65.21.137.238:27015", list[0].address)
        assertEquals("730", list[1].appId)
    }

    @Test
    fun `dedupe is case insensitive on address`() {
        writeInitialFile()
        val (added, skipped) = repo().add(listOf(FavoritesRepository.NewFavorite("x", "65.21.137.238:27015")))
        assertEquals(0, added)
        assertEquals(1, skipped)
    }
}
