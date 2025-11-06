package ru.workinprogress.telek.telegram.effect.handler

import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.entities.ChatId
import ru.workinprogress.telek.telegram.effect.EditMarkupEffect

class EditMarkupEffectHandler : TelegramEffectHandler<EditMarkupEffect> {
    override suspend fun handle(
        bot: Bot,
        effect: EditMarkupEffect,
    ) {
        bot.editMessageReplyMarkup(
            chatId = ChatId.fromId(effect.chatId),
            messageId = effect.messageId,
            replyMarkup = effect.markup,
        )
    }
}
