package ru.workinprogress.telek.telegram

import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.dispatcher.callbackQuery
import com.github.kotlintelegrambot.dispatcher.message
import ru.workinprogress.telek.Input
import ru.workinprogress.telek.Telek

fun com.github.kotlintelegrambot.dispatcher.Dispatcher.connect(telek: Telek) {
    fun ensureContext(bot: Bot) {
        telek.initIfNeeded(TelegramContext(bot))
    }

    message {
        ensureContext(bot)
        telek.onInput(
            chatId = message.chat.id,
            input =
                Input.Message(
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
                Input.Callback(
                    chatId = msg.chat.id,
                    messageId = msg.messageId,
                    data = callbackQuery.data,
                ),
        )
    }
}
