package io.github.youndie.telek.telegram

public fun telegramMessage(block: TelegramTextBuilder.() -> Unit): String =
    TelegramTextBuilder().apply(block).toString()

@DslMarker
public annotation class TelegramMessageDsl

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
