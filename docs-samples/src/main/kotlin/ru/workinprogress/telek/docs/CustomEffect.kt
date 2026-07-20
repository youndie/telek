// Compiled copy of README.md's "Defining a Custom Effect" section — keep both in sync.
package ru.workinprogress.telek.docs

import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.entities.ChatId
import ru.workinprogress.telek.Callback
import ru.workinprogress.telek.EffectFailed
import ru.workinprogress.telek.EffectResult
import ru.workinprogress.telek.EffectSuccess
import ru.workinprogress.telek.State
import ru.workinprogress.telek.TransitionBuilder
import ru.workinprogress.telek.telegram.TelegramContextSource
import ru.workinprogress.telek.telegram.effect.TelegramEffect
import ru.workinprogress.telek.telegram.effect.defaultEffectRegistry
import ru.workinprogress.telek.telegram.effect.handler.TelegramEffectHandler
import ru.workinprogress.telek.telegram.effect.telegramEffectExecutor
import ru.workinprogress.telek.transition

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
