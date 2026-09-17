package io.github.youndie.telek.telegram

import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.dispatcher.callbackQuery
import com.github.kotlintelegrambot.dispatcher.message
import io.github.youndie.telek.Callback
import io.github.youndie.telek.Keying
import io.github.youndie.telek.Message
import io.github.youndie.telek.Telek

/**
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
public fun com.github.kotlintelegrambot.dispatcher.Dispatcher.connect(
    telek: Telek,
    contextSource: TelegramContextSource,
    keying: Keying,
) {
    fun ensureContext(bot: Bot) {
        contextSource.provide(bot)
    }

    message {
        ensureContext(bot)
        telek.onInput(
            key = keying.key(message.chat.id, message.from?.id),
            input =
                Message(
                    chatId = message.chat.id,
                    text = message.text.orEmpty(),
                ),
        )
    }

    callbackQuery {
        ensureContext(bot)
        val msg = callbackQuery.message ?: return@callbackQuery
        // `callbackQuery.from`, not `msg.from`: the message carrying the button was sent by the
        // bot, and in a group anyone can press it.
        telek.onInput(
            key = keying.key(msg.chat.id, callbackQuery.from.id),
            input =
                Callback(
                    chatId = msg.chat.id,
                    messageId = msg.messageId,
                    data = callbackQuery.data,
                ),
        )
    }
}
