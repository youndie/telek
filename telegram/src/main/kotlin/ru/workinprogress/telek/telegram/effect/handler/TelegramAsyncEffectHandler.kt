package ru.workinprogress.telek.telegram.effect.handler

import com.github.kotlintelegrambot.Bot
import ru.workinprogress.telek.AsyncEffectHandler
import ru.workinprogress.telek.Event
import ru.workinprogress.telek.ExecutionContext
import ru.workinprogress.telek.telegram.TelegramContext
import ru.workinprogress.telek.telegram.effect.TelegramEffect

/** [TelegramEffectHandler]'s async counterpart — see [AsyncEffectHandler]. */
interface TelegramAsyncEffectHandler<T : TelegramEffect> : AsyncEffectHandler<T> {
    override suspend fun handle(
        context: ExecutionContext,
        effect: T,
    ): Event? {
        requireNotNull(context as? TelegramContext) {
            "TelegramEffect can only be executed in TelegramContext"
        }

        return handle(context.bot, effect)
    }

    suspend fun handle(
        bot: Bot,
        effect: T,
    ): Event?
}
