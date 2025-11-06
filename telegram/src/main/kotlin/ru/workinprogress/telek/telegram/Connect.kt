package ru.workinprogress.telek.telegram

import com.github.kotlintelegrambot.dispatcher.callbackQuery
import com.github.kotlintelegrambot.dispatcher.message
import ru.workinprogress.telek.Input
import ru.workinprogress.telek.Telek

fun com.github.kotlintelegrambot.dispatcher.Dispatcher.connect(telek: Telek) {
    message {
        telek.onInput(
            context = TelegramContext(bot),
            chatId = message.chat.id,
            input =
                Input.Message(
                    chatId = message.chat.id,
                    text = message.text.orEmpty(),
                ),
        )
    }

    callbackQuery {
        val msg = callbackQuery.message ?: return@callbackQuery
        telek.onInput(
            context = TelegramContext(bot),
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
