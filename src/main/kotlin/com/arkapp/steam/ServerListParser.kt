package com.arkapp.steam

data class ParsedServer(
    val name: String?,
    val host: String,
    val port: Int?,
) {
    fun address(defaultPort: Int): String = "$host:${port ?: defaultPort}"
}

data class ServerParseResult(
    val servers: List<ParsedServer>,
    val ignoredLines: List<String>,
)

/**
 * Tolerant extractor for server addresses pasted from anywhere (Discord lists,
 * web tables, chat messages). Scans free-form text instead of validating lines.
 */
object ServerListParser {

    // IPv4, optionally followed by a port. The separator is captured to apply the
    // whitespace-port heuristic below. Lookaheads keep "1.2.3.4.5" and adjacent
    // IPs ("1.2.3.4, 5.6.7.8") from swallowing each other.
    private val ipPattern = Regex(
        """(?<![\d.])((?:\d{1,3}\.){3}\d{1,3})(?!\.?\d)(?:(\s*:\s*|[\s,]+)(\d{1,5})(?![.\d]))?"""
    )

    // Hostnames only count with an explicit :port and an alphabetic TLD — otherwise
    // a name glued to an IP ("Earth65.21.137.238:27027") would read as a domain.
    private val hostPattern = Regex(
        """\b([a-zA-Z][a-zA-Z0-9-]*(?:\.[a-zA-Z0-9-]+)*\.[a-zA-Z]{2,})\s*:\s*(\d{1,5})(?![.\d])"""
    )

    private val nameSeparators = Regex("""[-–—|:;,·>*_#\[\]()"'`]+""")
    private const val MAX_NAME_LENGTH = 60

    fun parse(text: String): ServerParseResult {
        val servers = mutableListOf<ParsedServer>()
        val seen = mutableSetOf<String>()
        val ignored = mutableListOf<String>()

        for (rawLine in text.lines()) {
            val line = rawLine.trim()
            if (line.isEmpty()) continue

            data class Hit(val range: IntRange, val host: String, val port: Int?)

            val hits = mutableListOf<Hit>()
            for (m in ipPattern.findAll(line)) {
                val host = m.groupValues[1]
                if (!isValidIp(host)) continue
                val separator = m.groupValues[2]
                var port = m.groupValues[3].toIntOrNull()?.takeIf { it in 1..65535 }
                // A number after just whitespace/comma is only a port if it looks like
                // one (4-5 digits) — avoids eating slot counts like "1.2.3.4 (10 slots)".
                if (port != null && !separator.contains(':') && port < 1000) port = null
                val end = if (port != null) m.range.last else m.range.first + host.length - 1
                hits += Hit(m.range.first..end, host, port)
            }
            for (m in hostPattern.findAll(line)) {
                if (hits.any { it.range.first <= m.range.last && m.range.first <= it.range.last }) continue
                val port = m.groupValues[2].toIntOrNull()?.takeIf { it in 1..65535 } ?: continue
                hits += Hit(m.range, m.groupValues[1], port)
            }

            if (hits.isEmpty()) {
                ignored += line
                continue
            }

            val name = if (hits.size == 1) extractName(line, hits.single().range) else null
            for (hit in hits) {
                val key = "${hit.host.lowercase()}:${hit.port ?: -1}"
                if (!seen.add(key)) continue
                servers += ParsedServer(name, hit.host, hit.port)
            }
        }
        return ServerParseResult(servers, ignored)
    }

    private fun extractName(line: String, serverRange: IntRange): String? {
        val leftover = line.removeRange(serverRange.first..minOf(serverRange.last, line.length - 1))
        val cleaned = leftover
            .replace(nameSeparators, " ")
            .replace(Regex("""\s+"""), " ")
            .trim()
        return cleaned.takeIf { it.isNotEmpty() }?.take(MAX_NAME_LENGTH)
    }

    private fun isValidIp(ip: String): Boolean =
        ip.split('.').all { val n = it.toIntOrNull(); n != null && n in 0..255 }
}
