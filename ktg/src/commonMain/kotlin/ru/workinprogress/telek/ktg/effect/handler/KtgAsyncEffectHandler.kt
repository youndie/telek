package ru.workinprogress.telek.ktg.effect.handler

import dev.inmo.tgbotapi.bot.TelegramBot
import ru.workinprogress.telek.AsyncEffectHandler
import ru.workinprogress.telek.Event
import ru.workinprogress.telek.ExecutionContext
import ru.workinprogress.telek.ktg.KtgContext
import ru.workinprogress.telek.ktg.effect.KtgEffect

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
