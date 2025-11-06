package ru.workinprogress.telek.telegram.effect.handler

import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.entities.ChatId
import ru.workinprogress.telek.telegram.effect.SendMessageEffect

class SendMessageEffectHandler : TelegramEffectHandler<SendMessageEffect> {
    override suspend fun handle(
        bot: Bot,
        effect: SendMessageEffect,
    ) {
        bot.sendMessage(
            chatId = ChatId.fromId(effect.chatId),
            text = effect.text,
            replyMarkup = effect.markup,
        )
    }
}
