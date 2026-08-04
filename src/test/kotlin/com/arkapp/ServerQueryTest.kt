package com.arkapp

import com.arkapp.steam.ServerQuery
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ServerQueryTest {

    private fun infoPacket(name: String): ByteArray =
        byteArrayOf(-1, -1, -1, -1, 0x49, 0x11) + name.toByteArray(Charsets.UTF_8) + 0 +
            "TheIsland".toByteArray(Charsets.UTF_8) + 0

    @Test
    fun `parses server name from A2S_INFO response`() {
        assertEquals(
            "Prodigiosos PVP x10 - (v358.17)",
            ServerQuery.parseInfoName(infoPacket("Prodigiosos PVP x10 - (v358.17)")),
        )
    }

    @Test
    fun `keeps utf8 characters`() {
        assertEquals("Español Ñandú", ServerQuery.parseInfoName(infoPacket("Español Ñandú")))
    }

    @Test
    fun `returns null for blank or truncated payloads`() {
        assertNull(ServerQuery.parseInfoName(infoPacket("   ")))
        assertNull(ServerQuery.parseInfoName(byteArrayOf(-1, -1, -1, -1, 0x49)))
    }
}
