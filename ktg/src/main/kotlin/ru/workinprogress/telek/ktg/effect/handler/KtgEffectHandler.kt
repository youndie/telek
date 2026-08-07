package ru.workinprogress.telek.ktg.effect.handler

import dev.inmo.tgbotapi.bot.TelegramBot
import ru.workinprogress.telek.EffectHandler
import ru.workinprogress.telek.EffectResult
import ru.workinprogress.telek.ExecutionContext
import ru.workinprogress.telek.ktg.KtgContext
import ru.workinprogress.telek.ktg.effect.KtgEffect

/**
 * ktgbotapi reports API failures by throwing (`TelegramBot.execute` is documented as "can throw
 * almost any exception"), so handlers here don't return a failure [EffectResult] of their own —
 * they let the exception reach [ru.workinprogress.telek.EffectExecutorImpl], which logs it and
 * turns it into an [ru.workinprogress.telek.EffectFailed] that reaches
 * [ru.workinprogress.telek.TelekInterceptor.onError].
 */
interface KtgEffectHandler<T : KtgEffect> : EffectHandler<T> {
    override suspend fun handle(
        context: ExecutionContext,
        effect: T,
    ): EffectResult {
        requireNotNull(context as? KtgContext) {
            "KtgEffect can only be executed in KtgContext"
        }

        return handle(context.bot, effect)
    }

    suspend fun handle(
        bot: TelegramBot,
        effect: T,
    ): EffectResult
}
