package io.github.youndie.telek.telegram

import com.github.kotlintelegrambot.entities.InlineKeyboardMarkup
import io.github.youndie.telek.MessageText
import io.github.youndie.telek.MessageTextBuilder
import io.github.youndie.telek.State
import io.github.youndie.telek.TransitionBuilder
import io.github.youndie.telek.telegram.effect.EditMarkdownMessageEffect
import io.github.youndie.telek.telegram.effect.EditMarkupEffect
import io.github.youndie.telek.telegram.effect.EditMessageEffect
import io.github.youndie.telek.telegram.effect.SendMarkdownMessageEffect
import io.github.youndie.telek.telegram.effect.SendMessageEffect

public fun <S : State> TransitionBuilder<S>.sendMessage(
    chatId: Long,
    message: MessageTextBuilder.() -> Unit,
    keyboard: (InlineKeyboardBuilder.() -> Unit)? = null,
) {
    sendMessage(
        chatId = chatId,
        message = MessageTextBuilder().apply(message).build(),
        markup = keyboard?.let { InlineKeyboardBuilder().apply(it).build() },
    )
}

public fun <S : State> TransitionBuilder<S>.editMessage(
    chatId: Long,
    messageId: Long,
    message: MessageTextBuilder.() -> Unit,
    keyboard: (InlineKeyboardBuilder.() -> Unit)? = null,
) {
    editMessage(
        chatId = chatId,
        messageId = messageId,
        message = MessageTextBuilder().apply(message).build(),
        markup = keyboard?.let { InlineKeyboardBuilder().apply(it).build() },
    )
}

public fun <S : State> TransitionBuilder<S>.sendMessage(
    chatId: Long,
    message: MessageText,
    markup: InlineKeyboardMarkup? = null,
) {
    add(SendMessageEffect(chatId, message, markup))
}

public fun <S : State> TransitionBuilder<S>.editMessage(
    chatId: Long,
    messageId: Long,
    message: MessageText,
    markup: InlineKeyboardMarkup? = null,
) {
    add(EditMessageEffect(chatId, messageId, message, markup))
}

public fun <S : State> TransitionBuilder<S>.editMarkup(
    chatId: Long,
    messageId: Long,
    markup: InlineKeyboardMarkup? = null,
) {
    add(EditMarkupEffect(chatId, messageId, markup))
}

/**
 * Sends [text] for Telegram to parse as legacy Markdown — what every message did before entities.
 *
 * Kept for one release so a bot with hand-written Markdown keeps rendering while it migrates: had
 * this overload started sending the string as plain text instead, every asterisk in every message
 * would have become literal on upgrade, with nothing failing.
 *
 * `MessageText.plain(text)` is the other reading — send it literally, markup and all — and it is
 * the right one for anything a person typed.
 */
@Deprecated(
    "Telegram parses this string, so foreign text in it must be escaped and unbalanced markup " +
        "fails the whole message. Use sendMessage(chatId, message = { ... }), or " +
        "MessageText.plain(text) to send it literally.",
    ReplaceWith("sendMessage(chatId, MessageText.plain(text), markup)"),
    DeprecationLevel.WARNING,
)
@Suppress("DEPRECATION")
public fun <S : State> TransitionBuilder<S>.sendMessage(
    chatId: Long,
    text: String,
    markup: InlineKeyboardMarkup? = null,
) {
    add(SendMarkdownMessageEffect(chatId, text, markup))
}

/** The edit half of the deprecated [sendMessage] string overload; the same caveats apply. */
@Deprecated(
    "Telegram parses this string, so foreign text in it must be escaped and unbalanced markup " +
        "fails the whole message. Use editMessage(chatId, messageId, message = { ... }), or " +
        "MessageText.plain(text) to send it literally.",
    ReplaceWith("editMessage(chatId, messageId, MessageText.plain(text), markup)"),
    DeprecationLevel.WARNING,
)
@Suppress("DEPRECATION")
public fun <S : State> TransitionBuilder<S>.editMessage(
    chatId: Long,
    messageId: Long,
    text: String,
    markup: InlineKeyboardMarkup? = null,
) {
    add(EditMarkdownMessageEffect(chatId, messageId, text, markup))
}
