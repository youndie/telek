package ru.workinprogress.telek.ktg

fun telegramMessage(block: TelegramTextBuilder.() -> Unit): String = TelegramTextBuilder().apply(block).toString()

@DslMarker
annotation class TelegramMessageDsl

/**
 * Deliberately a verbatim copy of `:telegram`'s builder of the same name: it only produces a
 * Telegram-flavoured markdown [String] and has nothing to do with either client library, but
 * sharing it would mean `:ktg` depending on `:telegram` (and so on kotlin-telegram-bot). Keep the
 * two in sync when either changes.
 */
@TelegramMessageDsl
class TelegramTextBuilder {
    private val parts = StringBuilder()

    fun text(value: String) = parts.append(value)

    fun bold(value: String) = parts.append("*$value*")

    fun br() = parts.append("\n")

    fun br2() = parts.append("\n\n")

    fun row(block: TelegramTextBuilder.() -> Unit) {
        if (parts.isNotEmpty() && parts.lastOrNull()?.toString()?.endsWith("\n") != true) {
            parts.append("\n")
        }
        block()
        parts.append("\n")
    }

    fun <T> list(
        items: List<T>,
        block: TelegramTextBuilder.(T) -> Unit,
    ) = items.forEachIndexed { idx, item ->
        block(item)
        if (idx != items.lastIndex) br2()
    }

    fun build() = parts.toString().trimIndent()

    override fun toString(): String = build()
}
