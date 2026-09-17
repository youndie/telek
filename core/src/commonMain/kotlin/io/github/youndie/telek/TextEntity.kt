package io.github.youndie.telek

/**
 * A [MessageText] flattened the way the Bot API wants it: plain text plus ranges over it.
 *
 * Here rather than in a transport because the arithmetic is the risky part and it is the same
 * arithmetic for everyone. `:telegram` sends these almost verbatim; `:ktg` does not need them,
 * because ktgbotapi flattens its own `TextSource` list — but the offsets can be tested in `:core`,
 * on the JVM and on native, without either client library present.
 *
 * **Offsets are in UTF-16 code units**, which is what Telegram counts and what Kotlin's `String`
 * already indexes in. An emoji outside the BMP is two units, and a length taken in characters
 * would put every entity after it in the wrong place — a bug that only shows up in messages that
 * contain one.
 */
public data class TextEntity(
    public val type: TextEntityType,
    public val offset: Int,
    public val length: Int,
    /** Set for [TextEntityType.TextLink] and nothing else. */
    public val url: String? = null,
    /** Set for [TextEntityType.Pre] when the block named a language. */
    public val language: String? = null,
)

public enum class TextEntityType {
    Bold,
    Italic,
    Underline,
    Strikethrough,
    Spoiler,
    Blockquote,
    ExpandableBlockquote,
    Code,
    Pre,
    TextLink,
}

/**
 * The entities for this message, in the order their ranges open.
 *
 * Nested pieces produce overlapping ranges, which is exactly what Telegram expects: bold inside a
 * blockquote is two entities over the same offsets, not one combined style. Empty ranges are
 * dropped — Telegram rejects an entity of zero length, and an empty `bold { }` is a caller's
 * accident rather than an instruction.
 */
public fun MessageText.entities(): List<TextEntity> {
    val out = mutableListOf<TextEntity>()
    var offset = 0

    fun walk(pieces: List<TextPiece>) {
        pieces.forEach { piece ->
            when (piece) {
                is TextPiece.Plain -> {
                    offset += piece.text.length
                }

                is TextPiece.Code -> {
                    if (piece.text.isNotEmpty()) {
                        out += TextEntity(TextEntityType.Code, offset, piece.text.length)
                    }
                    offset += piece.text.length
                }

                is TextPiece.CodeBlock -> {
                    if (piece.text.isNotEmpty()) {
                        out += TextEntity(TextEntityType.Pre, offset, piece.text.length, language = piece.language)
                    }
                    offset += piece.text.length
                }

                is TextPiece.Styled -> {
                    val start = offset
                    walk(piece.text)
                    if (offset > start) {
                        out += TextEntity(piece.style.asEntityType(), start, offset - start)
                    }
                }

                is TextPiece.Link -> {
                    val start = offset
                    walk(piece.text)
                    if (offset > start) {
                        out += TextEntity(TextEntityType.TextLink, start, offset - start, url = piece.url)
                    }
                }
            }
        }
    }

    walk(pieces)
    return out.sortedBy { it.offset }
}

private fun TextStyle.asEntityType(): TextEntityType =
    when (this) {
        TextStyle.Bold -> TextEntityType.Bold
        TextStyle.Italic -> TextEntityType.Italic
        TextStyle.Underline -> TextEntityType.Underline
        TextStyle.Strikethrough -> TextEntityType.Strikethrough
        TextStyle.Spoiler -> TextEntityType.Spoiler
        TextStyle.Blockquote -> TextEntityType.Blockquote
        TextStyle.ExpandableBlockquote -> TextEntityType.ExpandableBlockquote
    }
