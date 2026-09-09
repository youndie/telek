package io.github.youndie.telek.telegram.effect

import com.github.kotlintelegrambot.entities.InlineKeyboardMarkup
import io.github.youndie.telek.Effect

public interface TelegramEffect : Effect

public data class SendMessageEffect(
    val chatId: Long,
    val text: String,
    val markup: InlineKeyboardMarkup? = null,
) : TelegramEffect

public data class EditMessageEffect(
    val chatId: Long,
    val messageId: Long,
    val text: String,
    val markup: InlineKeyboardMarkup? = null,
) : TelegramEffect

public data class EditMarkupEffect(
    val chatId: Long,
    val messageId: Long,
    val markup: InlineKeyboardMarkup? = null,
) : TelegramEffect
