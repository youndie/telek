package io.github.youndie.telek.ktg

public fun telegramMessage(block: TelegramTextBuilder.() -> Unit): String =
    TelegramTextBuilder().apply(block).toString()

@DslMarker
public annotation class TelegramMessageDsl

/**
 * Deliberately a verbatim copy of `:telegram`'s builder of the same name: it only produces a
 * Telegram-flavoured markdown [String] and has nothing to do with either client library, but
 * sharing it would mean `:ktg` depending on `:telegram` (and so on kotlin-telegram-bot). Keep the
 * two in sync when either changes.
 */
@TelegramMessageDsl
public class TelegramTextBuilder {
    private val parts = StringBuilder()

    public fun text(value: String) {
        parts.append(value)
    }

    public fun bold(value: String) {
        parts.append("*$value*")
    }

    public fun br() {
        parts.append("\n")
    }

    public fun br2() {
        parts.append("\n\n")
    }

    public fun row(block: TelegramTextBuilder.() -> Unit) {
        if (parts.isNotEmpty() && parts.lastOrNull()?.toString()?.endsWith("\n") != true) {
            parts.append("\n")
        }
        block()
        parts.append("\n")
    }

    public fun <T> list(
        items: List<T>,
        block: TelegramTextBuilder.(T) -> Unit,
    ): Unit =
        items.forEachIndexed { idx, item ->
            block(item)
            if (idx != items.lastIndex) br2()
        }

    public fun build(): String = parts.toString().trimIndent()

    override fun toString(): String = build()
}
