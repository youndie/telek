package io.github.youndie.telek.ktg.effect.handler

import dev.inmo.tgbotapi.bot.TelegramBot
import io.github.youndie.telek.AsyncEffectHandler
import io.github.youndie.telek.Event
import io.github.youndie.telek.ExecutionContext
import io.github.youndie.telek.ktg.KtgContext
import io.github.youndie.telek.ktg.effect.KtgEffect

/** [KtgEffectHandler]'s async counterpart — see [AsyncEffectHandler]. */
interface KtgAsyncEffectHandler<T : KtgEffect> : AsyncEffectHandler<T> {
    override suspend fun handle(
        context: ExecutionContext,
        effect: T,
    ): Event? {
        requireNotNull(context as? KtgContext) {
            "KtgEffect can only be executed in KtgContext"
        }

        return handle(context.bot, effect)
    }

    suspend fun handle(
        bot: TelegramBot,
        effect: T,
    ): Event?
}
