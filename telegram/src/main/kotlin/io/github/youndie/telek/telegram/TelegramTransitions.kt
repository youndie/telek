package io.github.youndie.telek.telegram

import com.github.kotlintelegrambot.entities.InlineKeyboardMarkup
import io.github.youndie.telek.State
import io.github.youndie.telek.TransitionBuilder
import io.github.youndie.telek.telegram.effect.EditMarkupEffect
import io.github.youndie.telek.telegram.effect.EditMessageEffect
import io.github.youndie.telek.telegram.effect.SendMessageEffect

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
