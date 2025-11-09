package ru.workinprogress.telek.telegram.effect.handler

import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.entities.ChatId
import ru.workinprogress.telek.EffectResult
import ru.workinprogress.telek.EffectSuccess
import ru.workinprogress.telek.telegram.effect.EditMarkupEffect

class EditMarkupEffectHandler : TelegramEffectHandler<EditMarkupEffect> {
    override suspend fun handle(
        bot: Bot,
        effect: EditMarkupEffect,
    ): EffectResult =
        bot
            .editMessageReplyMarkup(
                chatId = ChatId.fromId(effect.chatId),
                messageId = effect.messageId,
                replyMarkup = effect.markup,
            ).let { (_, exception) ->
                if (exception == null) {
                    EffectSuccess
                } else {
                    EditMarkupEffectResult(effect.chatId, effect.messageId)
                }
            }
}
