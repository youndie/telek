// Compiled copy of README.md's "Defining a Custom Effect" section — keep both in sync.
package io.github.youndie.telek.docs

import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.entities.ChatId
import io.github.youndie.telek.Callback
import io.github.youndie.telek.EffectFailed
import io.github.youndie.telek.EffectResult
import io.github.youndie.telek.EffectSuccess
import io.github.youndie.telek.State
import io.github.youndie.telek.TransitionBuilder
import io.github.youndie.telek.telegram.TelegramContextSource
import io.github.youndie.telek.telegram.effect.TelegramEffect
import io.github.youndie.telek.telegram.effect.defaultEffectRegistry
import io.github.youndie.telek.telegram.effect.handler.TelegramEffectHandler
import io.github.youndie.telek.telegram.effect.telegramEffectExecutor
import io.github.youndie.telek.transition

// Define your custom effect
data class CustomEffect(
    val chatId: Long,
    val messageId: Long,
) : TelegramEffect

// Implement its handler
class CustomEffectHandler : TelegramEffectHandler<CustomEffect> {
    override suspend fun handle(
        bot: Bot,
        effect: CustomEffect,
    ): EffectResult =
        bot
            .deleteMessage(ChatId.fromId(effect.chatId), effect.messageId)
            .fold({ EffectSuccess }, { error -> EffectFailed(IllegalStateException(error.toString())) })
}

// DSL extension for transitions
fun <S : State> TransitionBuilder<S>.customEffect(
    chatId: Long,
    messageId: Long,
) {
    add(CustomEffect(chatId, messageId))
}

fun customEffectRegistrationSample(contextSource: TelegramContextSource) {
    val effectRegistry =
        defaultEffectRegistry().apply {
            register(CustomEffect::class, CustomEffectHandler())
        }

    telegramEffectExecutor(contextSource, effectRegistry)
}

fun customEffectUsageSample(input: Callback) =
    transition<ExampleState> {
        customEffect(input.chatId, input.messageId)
    }
