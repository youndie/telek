package ru.workinprogress.telek.telegram.effect.handler

import com.github.kotlintelegrambot.Bot
import ru.workinprogress.telek.EffectHandler
import ru.workinprogress.telek.ExecutionContext
import ru.workinprogress.telek.telegram.TelegramContext
import ru.workinprogress.telek.telegram.effect.TelegramEffect

interface TelegramEffectHandler<T : TelegramEffect> : EffectHandler<T> {
    override suspend fun handle(
        context: ExecutionContext,
        effect: T,
    ) {
        requireNotNull(context as? TelegramContext) {
            "TelegramEffect can only be executed in TelegramContext"
        }

        handle(context.bot, effect)
    }

    suspend fun handle(
        bot: Bot,
        effect: T,
    )
}
