package ru.workinprogress.telek.ktg

import dev.inmo.tgbotapi.extensions.api.answers.answerCallbackQuery
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onDataCallbackQuery
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onText
import ru.workinprogress.telek.Telek

/**
 * Adapts ktgbotapi updates into telek [ru.workinprogress.telek.Input]s, keyed by `chatId`, and
 * feeds the behaviour context's [dev.inmo.tgbotapi.bot.TelegramBot] to [contextSource].
 *
 * [connect] and [ktgEffectExecutor] must be given the *same* [KtgContextSource] instance.
 *
 * @param answerCallbackQueries answers every handled callback query so Telegram stops the client's
 * spinner (kotlin-telegram-bot does this on its own; ktgbotapi doesn't). Turn it off if a
 * dispatcher answers with its own text/alert.
 */
fun BehaviourContext.connect(
    telek: Telek,
    contextSource: KtgContextSource,
    answerCallbackQueries: Boolean = true,
) {
    contextSource.provide(bot)

    onText { message ->
        telek.onInput(
            chatId = message.telekChatId,
            input = message.asTelekInput(),
        )
    }

    onDataCallbackQuery { query ->
        if (answerCallbackQueries) {
            runCatching { answerCallbackQuery(query) }
        }
        val input = query.asTelekInput() ?: return@onDataCallbackQuery
        telek.onInput(
            chatId = input.chatId,
            input = input,
        )
    }
}
