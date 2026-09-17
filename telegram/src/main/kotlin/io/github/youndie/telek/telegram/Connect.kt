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
 * filed under. The default keys per person per chat, which is what a wizard wants and what a group
 * requires; pass [Keying.PerChat] for state the whole chat shares.
 */
public fun com.github.kotlintelegrambot.dispatcher.Dispatcher.connect(
    telek: Telek,
    contextSource: TelegramContextSource,
    keying: Keying = Keying.PerUserInChat,
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
