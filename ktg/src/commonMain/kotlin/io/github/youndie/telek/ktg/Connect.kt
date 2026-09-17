package io.github.youndie.telek.ktg

import dev.inmo.tgbotapi.extensions.api.answers.answerCallbackQuery
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onDataCallbackQuery
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onText
import io.github.youndie.telek.Keying
import io.github.youndie.telek.Telek

/**
 * Adapts ktgbotapi updates into telek [io.github.youndie.telek.Input]s and feeds the behaviour
 * context's [dev.inmo.tgbotapi.bot.TelegramBot] to [contextSource].
 *
 * [connect] and [ktgEffectExecutor] must be given the *same* [KtgContextSource] instance.
 *
 * @param answerCallbackQueries answers every handled callback query so Telegram stops the client's
 * spinner (kotlin-telegram-bot does this on its own; ktgbotapi doesn't). Turn it off if a
 * dispatcher answers with its own text/alert.
 * @param keying how an update becomes the [io.github.youndie.telek.ConversationKey] the state is
 * filed under. The default keys per person per chat, which is what a wizard wants and what a group
 * requires; pass [Keying.PerChat] for state the whole chat shares.
 */
public fun BehaviourContext.connect(
    telek: Telek,
    contextSource: KtgContextSource,
    answerCallbackQueries: Boolean = true,
    keying: Keying = Keying.PerUserInChat,
) {
    contextSource.provide(bot)

    onText { message ->
        telek.onInput(
            key = keying.key(message.telekChatId, message.telekUserId),
            input = message.asTelekInput(),
        )
    }

    onDataCallbackQuery { query ->
        if (answerCallbackQueries) {
            runCatching { answerCallbackQuery(query) }
        }
        val input = query.asTelekInput() ?: return@onDataCallbackQuery
        // The query's own `from`, not the message's: the message was sent by the bot, and in a
        // group anyone can press a button on it. Keying by the message's sender would file every
        // member's callback under the bot.
        telek.onInput(
            key = keying.key(input.chatId, query.from.id.chatId.long),
            input = input,
        )
    }
}
