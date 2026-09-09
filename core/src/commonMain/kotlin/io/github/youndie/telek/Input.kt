package io.github.youndie.telek

public interface Input {
    public val chatId: Long
}

public data class Message(
    override val chatId: Long,
    val text: String,
) : Input

public data class Callback(
    override val chatId: Long,
    val messageId: Long,
    val data: String,
) : Input
