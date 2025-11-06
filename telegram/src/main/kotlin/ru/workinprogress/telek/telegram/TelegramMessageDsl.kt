package ru.workinprogress.telek.telegram

fun telegramMessage(block: TelegramTextBuilder.() -> Unit): String = TelegramTextBuilder().apply(block).toString()

@DslMarker
annotation class TelegramMessageDsl

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
