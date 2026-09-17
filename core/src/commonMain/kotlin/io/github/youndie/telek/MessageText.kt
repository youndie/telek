package io.github.youndie.telek

/**
 * A message body as a document, not as a string a server parses.
 *
 * The distinction is the whole point. Telegram accepts either a string plus a `parse_mode` — where
 * markup is characters *inside* the text — or plain text plus entities, which are offsets pointing
 * at ranges of it. With the first, any foreign text the bot did not write (a name somebody typed,
 * a product title) can open markup that never closes, and Telegram rejects the **whole** message;
 * from the outside that is a button that does nothing. Escaping is the only defence, it belongs at
 * every call site, and it is silently wrong the day the parse mode changes.
 *
 * With a document there is nothing to escape, because nothing in the text is parsed. The defect
 * class is gone by construction rather than by vigilance, and every format Telegram has becomes
 * expressible at once instead of the six that legacy Markdown allows.
 *
 * This type is in `:core` for the same reason [Input] is: it describes Telegram's own model, not a
 * client library's. Each transport maps it to whatever its library takes — `:ktg` to ktgbotapi's
 * `TextSource`, `:telegram` to `(text, entities)` — and neither mapping is visible here.
 *
 * Build one with [message]; read it back with [plain].
 */
public data class MessageText(
    public val pieces: List<TextPiece>,
) {
    /**
     * The text without any markup — exactly what Telegram is sent alongside the entities, and what
     * a person sees if their client renders nothing.
     *
     * Here so a test can assert on what was said without knowing how it was styled, which is what
     * most of them want; asserting on the pieces is for tests about the styling itself.
     */
    public val plain: String
        get() = buildString { pieces.forEach { it.appendPlain(this) } }

    public companion object {
        public val Empty: MessageText = MessageText(emptyList())

        /** A body with no markup at all. Escaping is not a question that arises. */
        public fun plain(text: String): MessageText = MessageText(listOf(TextPiece.Plain(text)))
    }
}

/**
 * One node of a [MessageText].
 *
 * The set is Telegram's, minus what a library cannot offer everyone: custom emoji are gated behind
 * a username bought on Fragment or the bot owner's Premium, so a bot that used them would work for
 * its author and fail for whoever copied the code.
 *
 * [Styled] and [Link] nest, because Telegram's entities do — bold inside a blockquote is two
 * overlapping ranges over the same text. [Code] and [CodeBlock] do not: Telegram ignores entities
 * inside them, so allowing children here would be an API that quietly drops what it was given.
 */
public sealed interface TextPiece {
    /** Literal text, rendered as written. */
    public data class Plain(
        public val text: String,
    ) : TextPiece

    /** [text] with a style applied to all of it, including anything nested inside. */
    public data class Styled(
        public val style: TextStyle,
        public val text: List<TextPiece>,
    ) : TextPiece

    /** An inline link. [url] is sent as an entity and never appears in the text. */
    public data class Link(
        public val url: String,
        public val text: List<TextPiece>,
    ) : TextPiece

    /** Inline monospace. Does not nest — Telegram drops entities inside `code`. */
    public data class Code(
        public val text: String,
    ) : TextPiece

    /** A monospace block, optionally tagged with a language for the client's highlighter. */
    public data class CodeBlock(
        public val text: String,
        public val language: String? = null,
    ) : TextPiece
}

/** The styles that wrap other pieces. See [TextPiece.Code] for the two that do not. */
public enum class TextStyle {
    Bold,
    Italic,
    Underline,
    Strikethrough,
    Spoiler,
    Blockquote,

    /** A blockquote a client shows collapsed, with the rest behind a tap. */
    ExpandableBlockquote,
}

private fun TextPiece.appendPlain(into: StringBuilder) {
    when (this) {
        is TextPiece.Plain -> into.append(text)
        is TextPiece.Code -> into.append(text)
        is TextPiece.CodeBlock -> into.append(text)
        is TextPiece.Styled -> text.forEach { it.appendPlain(into) }
        is TextPiece.Link -> text.forEach { it.appendPlain(into) }
    }
}
