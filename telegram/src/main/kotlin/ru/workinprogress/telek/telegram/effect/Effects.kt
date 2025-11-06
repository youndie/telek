package ru.workinprogress.telek.telegram.effect

import com.github.kotlintelegrambot.entities.InlineKeyboardMarkup
import ru.workinprogress.telek.Effect

interface TelegramEffect : Effect

data class SendMessageEffect(
    val chatId: Long,
    val text: String,
    val markup: InlineKeyboardMarkup? = null,
) : TelegramEffect

data class EditMessageEffect(
    val chatId: Long,
    val messageId: Long,
    val text: String,
    val markup: InlineKeyboardMarkup? = null,
) : TelegramEffect

data class EditMarkupEffect(
    val chatId: Long,
    val messageId: Long,
    val markup: InlineKeyboardMarkup? = null,
) : TelegramEffect
