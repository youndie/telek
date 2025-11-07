package ru.workinprogress.telek.telegram

import com.github.kotlintelegrambot.entities.InlineKeyboardMarkup
import ru.workinprogress.telek.State
import ru.workinprogress.telek.TransitionBuilder
import ru.workinprogress.telek.telegram.effect.EditMarkupEffect
import ru.workinprogress.telek.telegram.effect.EditMessageEffect
import ru.workinprogress.telek.telegram.effect.SendMessageEffect

fun <S : State> TransitionBuilder<S>.sendMessage(
    chatId: Long,
    message: TelegramTextBuilder.() -> Unit,
    keyboard: (InlineKeyboardBuilder.() -> Unit)? = null,
) {
    sendMessage(
        chatId = chatId,
        text = TelegramTextBuilder().apply(message).build(),
        markup = keyboard?.let { InlineKeyboardBuilder().apply(it).build() },
    )
}

fun <S : State> TransitionBuilder<S>.editMessage(
    chatId: Long,
    messageId: Long,
    message: TelegramTextBuilder.() -> Unit,
    keyboard: (InlineKeyboardBuilder.() -> Unit)? = null,
) {
    editMessage(
        chatId = chatId,
        messageId = messageId,
        text = TelegramTextBuilder().apply(message).build(),
        markup = keyboard?.let { InlineKeyboardBuilder().apply(it).build() },
    )
}

fun <S : State> TransitionBuilder<S>.sendMessage(
    chatId: Long,
    text: String,
    markup: InlineKeyboardMarkup? = null,
) {
    add(SendMessageEffect(chatId, text, markup))
}

fun <S : State> TransitionBuilder<S>.editMessage(
    chatId: Long,
    messageId: Long,
    text: String,
    markup: InlineKeyboardMarkup? = null,
) {
    add(EditMessageEffect(chatId, messageId, text, markup))
}

fun <S : State> TransitionBuilder<S>.editMarkup(
    chatId: Long,
    messageId: Long,
    markup: InlineKeyboardMarkup? = null,
) {
    add(EditMarkupEffect(chatId, messageId, markup))
}
