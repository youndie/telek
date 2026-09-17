package io.github.youndie.telek

@DslMarker
public annotation class MessageTextDsl

/**
 * Builds a [MessageText].
 *
 * One builder, in `:core`, used by both transports. It replaces a `TelegramTextBuilder` that was
 * duplicated verbatim in `:ktg` and `:telegram` — the copy's own comment apologised for itself and
 * asked the next person to keep the two in sync, which is a job nothing checked.
 *
 * `text`, `bold`, `br`, `br2`, `row` and `list` are the ones the old builder had and behave as they
 * did, so a call site that only used those reads the same; the rest is what legacy Markdown could
 * not express.
 *
 * Nothing here escapes anything, because nothing downstream parses the text. A name with `_`, `*`,
 * `[` and a backtick in it goes through [text] and arrives exactly as typed.
 */
@MessageTextDsl
public class MessageTextBuilder {
    private val pieces = mutableListOf<TextPiece>()

    public fun text(value: String) {
        if (value.isNotEmpty()) pieces += TextPiece.Plain(value)
    }

    public fun bold(value: String): Unit = bold { text(value) }

    public fun bold(block: MessageTextBuilder.() -> Unit): Unit = styled(TextStyle.Bold, block)

    public fun italic(value: String): Unit = italic { text(value) }

    public fun italic(block: MessageTextBuilder.() -> Unit): Unit = styled(TextStyle.Italic, block)

    public fun underline(value: String): Unit = underline { text(value) }

    public fun underline(block: MessageTextBuilder.() -> Unit): Unit = styled(TextStyle.Underline, block)

    public fun strikethrough(value: String): Unit = strikethrough { text(value) }

    public fun strikethrough(block: MessageTextBuilder.() -> Unit): Unit = styled(TextStyle.Strikethrough, block)

    public fun spoiler(value: String): Unit = spoiler { text(value) }

    public fun spoiler(block: MessageTextBuilder.() -> Unit): Unit = styled(TextStyle.Spoiler, block)

    public fun blockquote(block: MessageTextBuilder.() -> Unit): Unit = styled(TextStyle.Blockquote, block)

    /** A quote the client shows collapsed. Long output belongs here rather than in the message body. */
    public fun expandableBlockquote(block: MessageTextBuilder.() -> Unit): Unit =
        styled(TextStyle.ExpandableBlockquote, block)

    /** Inline monospace. Does not nest — see [TextPiece.Code]. */
    public fun code(value: String) {
        pieces += TextPiece.Code(value)
    }

    /** A monospace block; [language] only tells the client's highlighter what to do. */
    public fun codeBlock(
        value: String,
        language: String? = null,
    ) {
        pieces += TextPiece.CodeBlock(value, language)
    }

    public fun link(
        url: String,
        value: String,
    ): Unit = link(url) { text(value) }

    public fun link(
        url: String,
        block: MessageTextBuilder.() -> Unit,
    ) {
        pieces += TextPiece.Link(url, MessageTextBuilder().apply(block).pieces.toList())
    }

    public fun br() {
        text("\n")
    }

    public fun br2() {
        text("\n\n")
    }

    /** The block's content on its own line, with a newline before it if one is not already there. */
    public fun row(block: MessageTextBuilder.() -> Unit) {
        if (pieces.isNotEmpty() && !endsWithNewline()) br()
        block()
        br()
    }

    /** Each item rendered by [block], separated by a blank line — the old builder's behaviour. */
    public fun <T> list(
        items: List<T>,
        block: MessageTextBuilder.(T) -> Unit,
    ): Unit =
        items.forEachIndexed { index, item ->
            block(item)
            if (index != items.lastIndex) br2()
        }

    public fun build(): MessageText = MessageText(pieces.toList())

    private fun styled(
        style: TextStyle,
        block: MessageTextBuilder.() -> Unit,
    ) {
        pieces += TextPiece.Styled(style, MessageTextBuilder().apply(block).pieces.toList())
    }

    private fun endsWithNewline(): Boolean = MessageText(pieces.toList()).plain.endsWith("\n")
}

/** Builds a [MessageText] outside a transition — useful when a message is assembled elsewhere. */
public fun message(block: MessageTextBuilder.() -> Unit): MessageText = MessageTextBuilder().apply(block).build()
