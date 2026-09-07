package io.github.youndie.telek.ktg.effect

import dev.inmo.tgbotapi.types.buttons.InlineKeyboardMarkup
import io.github.youndie.telek.Effect

interface KtgEffect : Effect

data class SendMessageEffect(
    val chatId: Long,
    val text: String,
    val markup: InlineKeyboardMarkup? = null,
) : KtgEffect

data class EditMessageEffect(
    val chatId: Long,
    val messageId: Long,
    val text: String,
    val markup: InlineKeyboardMarkup? = null,
) : KtgEffect

data class EditMarkupEffect(
    val chatId: Long,
    val messageId: Long,
    val markup: InlineKeyboardMarkup? = null,
) : KtgEffect
