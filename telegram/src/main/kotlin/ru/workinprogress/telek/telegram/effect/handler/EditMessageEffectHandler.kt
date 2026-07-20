package ru.workinprogress.telek.telegram.effect.handler

import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.ParseMode
import ru.workinprogress.telek.EffectFailed
import ru.workinprogress.telek.telegram.effect.EditMessageEffect

class EditMessageEffectHandler : TelegramEffectHandler<EditMessageEffect> {
    override suspend fun handle(
        bot: Bot,
        effect: EditMessageEffect,
    ) = bot
        .editMessageText(
            chatId = ChatId.fromId(effect.chatId),
            messageId = effect.messageId,
            text = effect.text,
            parseMode = ParseMode.MARKDOWN,
            replyMarkup = effect.markup,
        ).let { (_, exception) ->
            if (exception != null) {
                EffectFailed(exception)
            } else {
                EditMessageEffectResult(effect.chatId, effect.messageId)
            }
        }
}
