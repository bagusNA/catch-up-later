package com.bagusna.catchuplater.features.library.service

/**
 * Builds FTS5 MATCH expressions from untrusted user input.
 *
 * Every whitespace-separated token becomes a quoted prefix term (`"foo"*`), and
 * embedded double quotes are escaped by doubling them. Quoting means FTS
 * operators such as `AND`, `OR`, `NEAR`, `-`, or `:` are treated as literal
 * text, so malformed or injection-like input can never reach the FTS parser.
 */
object SearchQuery {
    fun toMatchExpression(query: String): String? {
        val tokens = query.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
        if (tokens.isEmpty()) return null
        return tokens.joinToString(" ") { token ->
            "\"${token.replace("\"", "\"\"")}\"*"
        }
    }
}
