package ru.workinprogress.telek.telegram.effect.handler

import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.entities.ChatId
import ru.workinprogress.telek.telegram.effect.EditMessageEffect

class EditMessageEffectHandler : TelegramEffectHandler<EditMessageEffect> {
    override suspend fun handle(
        bot: Bot,
        effect: EditMessageEffect,
    ) {
        bot.editMessageText(
            chatId = ChatId.fromId(effect.chatId),
            messageId = effect.messageId,
            text = effect.text,
        )
    }
}
