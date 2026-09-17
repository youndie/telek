package io.github.youndie.telek

/**
 * Something a user sent.
 *
 * [chatId] is the Telegram **address** — the chat a reply is sent to — and not the key the
 * conversation's state is filed under; in a group those are different things. See
 * [ConversationKey], and [Telek.onInput], which takes the key separately.
 */
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
