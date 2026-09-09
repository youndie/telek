package io.github.youndie.telek.telegram.effect.handler

import com.github.kotlintelegrambot.types.TelegramBotResult
import io.github.youndie.telek.EffectResult

public abstract class TelegramEffectSuccess : EffectResult {
    public abstract val chatId: Long
    public abstract val messageId: Long
}

public class SendMessageEffectResult(
    override val chatId: Long,
    override val messageId: Long,
) : TelegramEffectSuccess()

public class EditMessageEffectResult(
    override val chatId: Long,
    override val messageId: Long,
) : TelegramEffectSuccess()

public class EditMarkupEffectResult(
    override val chatId: Long,
    override val messageId: Long,
) : TelegramEffectSuccess()

public class TelegramEffectError(
    public val chatId: Long,
    public val error: TelegramBotResult.Error,
) : EffectResult
