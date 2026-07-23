package com.arkapp

import com.arkapp.steam.Vdf
import com.arkapp.steam.VdfObject
import com.arkapp.steam.VdfParseException
import com.arkapp.steam.VdfString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class VdfTest {

    // Mirrors the real serverbrowser_hist.vdf captured from the machine.
    private val sample = """
        "Filters"
        {
            "Favorites"
            {
                "1"
                {
                    "name"  "65.21.137.238:27015"
                    "address"  "65.21.137.238:27015"
                    "LastPlayed"  "0"
                    "appid"  "0"
                    "accountid"  "0"
                }
                "12"
                {
                    "name"  "65.21.137.238:27037"
                    "address"  "65.21.137.238:27037"
                    "LastPlayed"  "1784843954"
                    "appid"  "346110"
                    "accountid"  "0"
                }
            }
            "History"
            {
                "12"
                {
                    "name"  "65.21.137.238:27037"
                    "address"  "65.21.137.238:27037"
                    "LastPlayed"  "1784843954"
                    "appid"  "346110"
                    "accountid"  "0"
                }
            }
        }
    """.trimIndent()

    @Test
    fun `parses real favorites structure`() {
        val root = Vdf.parse(sample)
        val favorites = root.obj("Filters")?.obj("Favorites")
        assertNotNull(favorites)
        assertEquals(2, favorites.entries.size)
        assertEquals("1", favorites.entries[0].first)
        val first = favorites.entries[0].second as VdfObject
        assertEquals("65.21.137.238:27015", first.string("address"))
        assertEquals("0", first.string("appid"))
        val history = root.obj("Filters")?.obj("History")
        assertNotNull(history)
        assertEquals(1, history.entries.size)
    }

    @Test
    fun `lookup is case insensitive like Valve KeyValues`() {
        val root = Vdf.parse(sample)
        assertNotNull(root.obj("filters"))
        assertNotNull(root.obj("FILTERS")?.obj("favorites"))
        val entry = root.obj("Filters")!!.obj("Favorites")!!.entries[0].second as VdfObject
        assertEquals("0", entry.string("lastplayed"))
    }

    @Test
    fun `write then parse is stable (round-trip)`() {
        val canonical = Vdf.write(Vdf.parse(sample))
        assertEquals(canonical, Vdf.write(Vdf.parse(canonical)))
    }

    @Test
    fun `round-trip preserves order and duplicate keys`() {
        val text = "\"root\"\n{\n\"z\" \"1\"\n\"a\" \"2\"\n\"z\" \"3\"\n}"
        val root = Vdf.parse(text)
        val entries = root.obj("root")!!.entries
        assertEquals(listOf("z", "a", "z"), entries.map { it.first })
        assertEquals(listOf("1", "2", "3"), entries.map { (it.second as VdfString).value })
        val rewritten = Vdf.parse(Vdf.write(root)).obj("root")!!.entries
        assertEquals(listOf("z", "a", "z"), rewritten.map { it.first })
    }

    @Test
    fun `handles escapes and comments`() {
        val text = "// comment line\n\"key\\\"quoted\"  \"value with \\\\ backslash\"\n\"empty\"\n{\n}"
        val root = Vdf.parse(text)
        assertEquals("value with \\ backslash", root.string("key\"quoted"))
        assertNotNull(root.obj("empty"))
        assertEquals(0, root.obj("empty")!!.entries.size)
        // Escapes survive a rewrite
        val reparsed = Vdf.parse(Vdf.write(root))
        assertEquals("value with \\ backslash", reparsed.string("key\"quoted"))
    }

    @Test
    fun `parses unquoted tokens`() {
        val root = Vdf.parse("key value\nobj\n{\ninner 42\n}")
        assertEquals("value", root.string("key"))
        assertEquals("42", root.obj("obj")?.string("inner"))
    }

    @Test
    fun `getOrCreateObj creates missing sections`() {
        val root = VdfObject()
        val favs = root.getOrCreateObj("Filters").getOrCreateObj("Favorites")
        favs.entries += "1" to VdfString("x")
        val reparsed = Vdf.parse(Vdf.write(root))
        assertEquals("x", reparsed.obj("Filters")?.obj("Favorites")?.string("1"))
    }

    @Test
    fun `rejects malformed input`() {
        assertFailsWith<VdfParseException> { Vdf.parse("\"unclosed") }
        assertFailsWith<VdfParseException> { Vdf.parse("\"a\"\n{\n\"b\" \"c\"") }
        assertNull(runCatching { Vdf.parse("}") }.getOrNull())
    }
}
