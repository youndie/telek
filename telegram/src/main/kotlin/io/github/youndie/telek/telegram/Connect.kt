package io.github.youndie.telek.telegram

import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.dispatcher.callbackQuery
import com.github.kotlintelegrambot.dispatcher.message
import io.github.youndie.telek.Callback
import io.github.youndie.telek.Message
import io.github.youndie.telek.Telek

public fun com.github.kotlintelegrambot.dispatcher.Dispatcher.connect(
    telek: Telek,
    contextSource: TelegramContextSource,
) {
    fun ensureContext(bot: Bot) {
        contextSource.provide(bot)
    }

    message {
        ensureContext(bot)
        telek.onInput(
            chatId = message.chat.id,
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
        telek.onInput(
            chatId = msg.chat.id,
            input =
                Callback(
                    chatId = msg.chat.id,
                    messageId = msg.messageId,
                    data = callbackQuery.data,
                ),
        )
    }
}
