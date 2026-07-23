package com.arkapp.steam

class VdfParseException(message: String) : Exception(message)

sealed interface VdfValue

data class VdfString(val value: String) : VdfValue

/**
 * Ordered key/value container. Preserves entry order and duplicate keys so that
 * files we don't fully own (History, other games' favorites) round-trip untouched.
 * Key lookups are case-insensitive, matching Valve's KeyValues behavior.
 */
class VdfObject(
    val entries: MutableList<Pair<String, VdfValue>> = mutableListOf(),
) : VdfValue {
    fun get(key: String): VdfValue? = entries.firstOrNull { it.first.equals(key, ignoreCase = true) }?.second
    fun obj(key: String): VdfObject? = get(key) as? VdfObject
    fun string(key: String): String? = (get(key) as? VdfString)?.value
    fun getOrCreateObj(key: String): VdfObject = obj(key) ?: VdfObject().also { entries += key to it }
}

object Vdf {

    private enum class TokenType { STRING, OPEN, CLOSE }
    private data class Token(val type: TokenType, val value: String = "")

    fun parse(text: String): VdfObject {
        val tokens = tokenize(text)
        var i = 0

        fun parseInto(obj: VdfObject, topLevel: Boolean) {
            while (i < tokens.size) {
                val t = tokens[i]
                when (t.type) {
                    TokenType.CLOSE -> {
                        if (topLevel) throw VdfParseException("Unexpected '}'")
                        i++
                        return
                    }
                    TokenType.OPEN -> throw VdfParseException("Unexpected '{'")
                    TokenType.STRING -> {
                        val key = t.value
                        i++
                        val next = tokens.getOrNull(i) ?: throw VdfParseException("Unexpected end after key \"$key\"")
                        when (next.type) {
                            TokenType.OPEN -> {
                                i++
                                val child = VdfObject()
                                obj.entries += key to child
                                parseInto(child, topLevel = false)
                            }
                            TokenType.STRING -> {
                                i++
                                obj.entries += key to VdfString(next.value)
                            }
                            TokenType.CLOSE -> throw VdfParseException("Key \"$key\" has no value")
                        }
                    }
                }
            }
            if (!topLevel) throw VdfParseException("Missing closing '}'")
        }

        val root = VdfObject()
        parseInto(root, topLevel = true)
        return root
    }

    private fun tokenize(text: String): List<Token> {
        val tokens = mutableListOf<Token>()
        var i = 0
        while (i < text.length) {
            val c = text[i]
            when {
                c.isWhitespace() -> i++
                c == '/' && i + 1 < text.length && text[i + 1] == '/' -> {
                    while (i < text.length && text[i] != '\n') i++
                }
                c == '{' -> { tokens += Token(TokenType.OPEN); i++ }
                c == '}' -> { tokens += Token(TokenType.CLOSE); i++ }
                c == '"' -> {
                    i++
                    val sb = StringBuilder()
                    while (i < text.length && text[i] != '"') {
                        if (text[i] == '\\' && i + 1 < text.length) {
                            when (val e = text[i + 1]) {
                                '"' -> sb.append('"')
                                '\\' -> sb.append('\\')
                                'n' -> sb.append('\n')
                                't' -> sb.append('\t')
                                else -> { sb.append('\\'); sb.append(e) }
                            }
                            i += 2
                        } else {
                            sb.append(text[i])
                            i++
                        }
                    }
                    if (i >= text.length) throw VdfParseException("Unterminated string")
                    i++
                    tokens += Token(TokenType.STRING, sb.toString())
                }
                else -> {
                    val start = i
                    while (i < text.length && !text[i].isWhitespace() &&
                        text[i] != '{' && text[i] != '}' && text[i] != '"'
                    ) i++
                    tokens += Token(TokenType.STRING, text.substring(start, i))
                }
            }
        }
        return tokens
    }

    fun write(root: VdfObject): String = buildString {
        root.entries.forEach { writeEntry(it, 0) }
    }

    private fun StringBuilder.writeEntry(entry: Pair<String, VdfValue>, depth: Int) {
        val indent = "\t".repeat(depth)
        val key = escape(entry.first)
        when (val v = entry.second) {
            is VdfString ->
                append(indent).append('"').append(key).append("\"\t\t\"").append(escape(v.value)).append("\"\n")
            is VdfObject -> {
                append(indent).append('"').append(key).append("\"\n")
                append(indent).append("{\n")
                v.entries.forEach { writeEntry(it, depth + 1) }
                append(indent).append("}\n")
            }
        }
    }

    private fun escape(s: String) = s.replace("\\", "\\\\").replace("\"", "\\\"")
}
