package com.arkapp.steam

import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

/**
 * Minimal A2S_INFO client (Valve query protocol, UDP on the query port).
 * ARK servers answer with their real name. All failures return null —
 * the app must behave identically without network access.
 */
object ServerQuery {

    private val REQUEST =
        byteArrayOf(-1, -1, -1, -1, 0x54) + "Source Engine Query".toByteArray(Charsets.US_ASCII) + 0

    fun queryName(host: String, port: Int, timeoutMs: Int = 1200): String? = runCatching {
        DatagramSocket().use { socket ->
            socket.soTimeout = timeoutMs
            val addr = InetAddress.getByName(host)
            var payload = REQUEST
            // First reply may be an A2S_SERVERQUERY_GETCHALLENGE (0x41); resend with the challenge.
            repeat(3) {
                socket.send(DatagramPacket(payload, payload.size, addr, port))
                val buf = ByteArray(4096)
                val pkt = DatagramPacket(buf, buf.size)
                socket.receive(pkt)
                val data = buf.copyOf(pkt.length)
                when {
                    data.size >= 6 && data[4] == 0x49.toByte() -> return parseInfoName(data)
                    data.size >= 9 && data[4] == 0x41.toByte() ->
                        payload = REQUEST + data.copyOfRange(5, 9)
                    else -> return null
                }
            }
            null
        }
    }.getOrNull()

    /** Parses the server name out of an A2S_INFO response (header 4 + type 1 + protocol 1, then name\0). */
    fun parseInfoName(data: ByteArray): String? {
        val start = 6
        if (data.size <= start) return null
        val end = (start until data.size).firstOrNull { data[it] == 0.toByte() } ?: data.size
        return String(data, start, end - start, Charsets.UTF_8).trim().ifBlank { null }
    }
}
