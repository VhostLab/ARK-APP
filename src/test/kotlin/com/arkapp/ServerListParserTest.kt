package com.arkapp

import com.arkapp.steam.ServerListParser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ServerListParserTest {

    @Test
    fun `simple address with port`() {
        val result = ServerListParser.parse("65.21.137.238:27015")
        assertEquals(1, result.servers.size)
        assertEquals("65.21.137.238", result.servers[0].host)
        assertEquals(27015, result.servers[0].port)
    }

    @Test
    fun `messy discord-style list`() {
        val text = """
            🦖 Ragnarok x25 - 65.21.137.238:27015
            Island > 65.21.137.238 27017
            65.21.137.238,27019 (mapa Aberration)

            servidor nuevo 10.0.0.1
        """.trimIndent()
        val result = ServerListParser.parse(text)
        assertEquals(4, result.servers.size)

        assertEquals(27015, result.servers[0].port)
        assertTrue(result.servers[0].name!!.contains("Ragnarok"))

        assertEquals(27017, result.servers[1].port)
        assertTrue(result.servers[1].name!!.contains("Island"))

        assertEquals(27019, result.servers[2].port)
        assertTrue(result.servers[2].name!!.contains("Aberration"))

        assertNull(result.servers[3].port)
        assertEquals("10.0.0.1", result.servers[3].host)
        assertEquals(0, result.ignoredLines.size)
    }

    @Test
    fun `multiple addresses in one line get no name`() {
        val result = ServerListParser.parse("cluster: 1.2.3.4:27015 1.2.3.4:27017")
        assertEquals(2, result.servers.size)
        assertNull(result.servers[0].name)
        assertNull(result.servers[1].name)
    }

    @Test
    fun `comma separated ips do not swallow each other`() {
        val result = ServerListParser.parse("1.2.3.4, 5.6.7.8")
        assertEquals(2, result.servers.size)
        assertEquals("1.2.3.4", result.servers[0].host)
        assertNull(result.servers[0].port)
        assertEquals("5.6.7.8", result.servers[1].host)
    }

    @Test
    fun `small numbers after whitespace are not ports`() {
        val result = ServerListParser.parse("1.2.3.4 25 slots")
        assertEquals(1, result.servers.size)
        assertNull(result.servers[0].port)
    }

    @Test
    fun `colon port is always accepted`() {
        val result = ServerListParser.parse("1.2.3.4:80")
        assertEquals(80, result.servers[0].port)
    }

    @Test
    fun `invalid octets are ignored`() {
        val result = ServerListParser.parse("999.1.1.1:27015\nvalid 1.2.3.4:27015")
        assertEquals(1, result.servers.size)
        assertEquals("1.2.3.4", result.servers[0].host)
        assertEquals(1, result.ignoredLines.size)
    }

    @Test
    fun `duplicates within pasted text are removed`() {
        val result = ServerListParser.parse("1.2.3.4:27015\notra vez 1.2.3.4:27015")
        assertEquals(1, result.servers.size)
    }

    @Test
    fun `hostname with explicit port`() {
        val result = ServerListParser.parse("play.miark.es:27015")
        assertEquals(1, result.servers.size)
        assertEquals("play.miark.es", result.servers[0].host)
        assertEquals(27015, result.servers[0].port)
    }

    @Test
    fun `hostname without port is ignored`() {
        val result = ServerListParser.parse("hola mundo servidor")
        assertEquals(0, result.servers.size)
        assertEquals(1, result.ignoredLines.size)
    }

    @Test
    fun `address inside longer dotted sequence is not an ip`() {
        val result = ServerListParser.parse("version 1.2.3.4.5 del mod")
        assertEquals(0, result.servers.size)
    }

    @Test
    fun `real cluster list including names glued to the ip`() {
        val text = """
            The Island 65.21.137.238:27015
            Valguero 65.21.137.238:27029
            Scorched Earth65.21.137.238:27027
            Ragnarok 65.21.137.238:27017
            Lost Island 65.21.137.238:27031
            Genesis 2 65.21.137.238:27037
            Genesis 1 65.21.137.238:27025
            Fjordur 65.21.137.238:27023
            Extinction 65.21.137.238:27021
            Crstal Isles 65.21.137.238:27033
            Aberration 65.21.137.238:27035
            The Center65.21.137.238:27019
        """.trimIndent()
        val result = ServerListParser.parse(text)
        assertEquals(12, result.servers.size)
        assertTrue(result.servers.all { it.host == "65.21.137.238" })
        assertEquals(
            listOf(27015, 27029, 27027, 27017, 27031, 27037, 27025, 27023, 27021, 27033, 27035, 27019),
            result.servers.map { it.port },
        )
        assertEquals("The Island", result.servers[0].name)
        assertEquals("Scorched Earth", result.servers[2].name)
        assertEquals("Genesis 2", result.servers[5].name)
        assertEquals("The Center", result.servers[11].name)
        assertEquals(0, result.ignoredLines.size)
    }

    @Test
    fun `default port fills missing port in address`() {
        val result = ServerListParser.parse("10.0.0.1")
        assertEquals("10.0.0.1:27015", result.servers[0].address(27015))
    }
}
