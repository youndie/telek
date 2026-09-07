package io.github.youndie.telek.telegram.effect.handler

import com.github.kotlintelegrambot.Bot
import io.github.youndie.telek.EffectHandler
import io.github.youndie.telek.EffectResult
import io.github.youndie.telek.ExecutionContext
import io.github.youndie.telek.telegram.TelegramContext
import io.github.youndie.telek.telegram.effect.TelegramEffect

interface TelegramEffectHandler<T : TelegramEffect> : EffectHandler<T> {
    override suspend fun handle(
        context: ExecutionContext,
        effect: T,
    ): EffectResult {
        requireNotNull(context as? TelegramContext) {
            "TelegramEffect can only be executed in TelegramContext"
        }

        return handle(context.bot, effect)
    }

    suspend fun handle(
        bot: Bot,
        effect: T,
    ): EffectResult
}
