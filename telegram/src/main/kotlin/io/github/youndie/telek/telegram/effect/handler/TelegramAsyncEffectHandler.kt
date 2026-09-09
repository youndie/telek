package io.github.youndie.telek.telegram.effect.handler

import com.github.kotlintelegrambot.Bot
import io.github.youndie.telek.AsyncEffectHandler
import io.github.youndie.telek.Event
import io.github.youndie.telek.ExecutionContext
import io.github.youndie.telek.telegram.TelegramContext
import io.github.youndie.telek.telegram.effect.TelegramEffect

/** [TelegramEffectHandler]'s async counterpart — see [AsyncEffectHandler]. */
public interface TelegramAsyncEffectHandler<T : TelegramEffect> : AsyncEffectHandler<T> {
    override suspend fun handle(
        context: ExecutionContext,
        effect: T,
    ): Event? {
        requireNotNull(context as? TelegramContext) {
            "TelegramEffect can only be executed in TelegramContext"
        }

        return handle(context.bot, effect)
    }

    public suspend fun handle(
        bot: Bot,
        effect: T,
    ): Event?
}
