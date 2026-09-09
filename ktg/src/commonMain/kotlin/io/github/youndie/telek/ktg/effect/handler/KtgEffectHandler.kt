package io.github.youndie.telek.ktg.effect.handler

import dev.inmo.tgbotapi.bot.TelegramBot
import io.github.youndie.telek.EffectHandler
import io.github.youndie.telek.EffectResult
import io.github.youndie.telek.ExecutionContext
import io.github.youndie.telek.ktg.KtgContext
import io.github.youndie.telek.ktg.effect.KtgEffect

/**
 * ktgbotapi reports API failures by throwing (`TelegramBot.execute` is documented as "can throw
 * almost any exception"), so handlers here don't return a failure [EffectResult] of their own —
 * they let the exception reach [io.github.youndie.telek.EffectExecutorImpl], which logs it and
 * turns it into an [io.github.youndie.telek.EffectFailed] that reaches
 * [io.github.youndie.telek.TelekInterceptor.onError].
 */
public interface KtgEffectHandler<T : KtgEffect> : EffectHandler<T> {
    override suspend fun handle(
        context: ExecutionContext,
        effect: T,
    ): EffectResult {
        requireNotNull(context as? KtgContext) {
            "KtgEffect can only be executed in KtgContext"
        }

        return handle(context.bot, effect)
    }

    public suspend fun handle(
        bot: TelegramBot,
        effect: T,
    ): EffectResult
}
