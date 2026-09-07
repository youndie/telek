package io.github.youndie.telek.telegram.effect.handler

import com.github.kotlintelegrambot.types.TelegramBotResult
import io.github.youndie.telek.EffectResult

abstract class TelegramEffectSuccess : EffectResult {
    abstract val chatId: Long
    abstract val messageId: Long
}

class SendMessageEffectResult(
    override val chatId: Long,
    override val messageId: Long,
) : TelegramEffectSuccess()

class EditMessageEffectResult(
    override val chatId: Long,
    override val messageId: Long,
) : TelegramEffectSuccess()

class EditMarkupEffectResult(
    override val chatId: Long,
    override val messageId: Long,
) : TelegramEffectSuccess()

class TelegramEffectError(
    val chatId: Long,
    val error: TelegramBotResult.Error,
) : EffectResult
