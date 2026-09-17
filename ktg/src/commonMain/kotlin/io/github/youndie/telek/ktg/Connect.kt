package io.github.youndie.telek.ktg

import dev.inmo.tgbotapi.extensions.api.answers.answerCallbackQuery
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onContact
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onDataCallbackQuery
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onDocument
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onLocation
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onPhoto
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onText
import io.github.youndie.telek.Keying
import io.github.youndie.telek.Telek

/**
 * Adapts ktgbotapi updates into telek [io.github.youndie.telek.Input]s and feeds the behaviour
 * context's [dev.inmo.tgbotapi.bot.TelegramBot] to [contextSource].
 *
 * Subscribes text, data callbacks, photos, documents, contacts and locations. A content type telek
 * does not model is not subscribed and therefore never reaches the FSM — a bot that needs one wires
 * its own trigger and calls [io.github.youndie.telek.Telek.onInput] with an `Input` of its own; the
 * adapters here are public so that path reuses them.
 *
 * [connect] and [ktgEffectExecutor] must be given the *same* [KtgContextSource] instance.
 *
 * @param answerCallbackQueries answers every handled callback query so Telegram stops the client's
 * spinner (kotlin-telegram-bot does this on its own; ktgbotapi doesn't). Turn it off if a
 * dispatcher answers with its own text/alert.
 * @param keying how an update becomes the [io.github.youndie.telek.ConversationKey] the state is
 * filed under, and **required on purpose**. [Keying.PerUserInChat] is what a wizard wants and what a
 * group requires; [Keying.PerChat] is for state the whole chat shares — a poll, a group game.
 *
 * It has no default because a default made this decision silently for a bot that merely upgraded:
 * the key changed under it, stored state stopped being found, and a bot that also feeds some inputs
 * through [Telek.onInput] directly ended up with one conversation split across two keys in the same
 * process. None of that has a symptom anyone would trace back to a parameter they never typed. A
 * required parameter costs a new bot one word and turns that into a compile error at the exact seam
 * that decides it.
 */
public fun BehaviourContext.connect(
    telek: Telek,
    contextSource: KtgContextSource,
    keying: Keying,
    answerCallbackQueries: Boolean = true,
) {
    contextSource.provide(bot)

    onText { message ->
        telek.onInput(
            key = keying.key(message.telekChatId, message.telekUserId),
            input = message.asTelekInput(),
        )
    }

    // One subscription per input type telek models. They are separate `on*` triggers rather than
    // one `onContentMessage` with a `when`, because ktgbotapi's own filtering is what decides which
    // updates a handler is offered — collapsing them into one would mean re-implementing that
    // filtering here and silently swallowing every content type telek does not model.
    onPhoto { message ->
        telek.onInput(
            key = keying.key(message.telekChatId, message.telekUserId),
            input = message.asTelekInput(),
        )
    }

    onDocument { message ->
        telek.onInput(
            key = keying.key(message.telekChatId, message.telekUserId),
            input = message.asTelekInput(),
        )
    }

    onContact { message ->
        telek.onInput(
            key = keying.key(message.telekChatId, message.telekUserId),
            input = message.asTelekInput(),
        )
    }

    onLocation { message ->
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
