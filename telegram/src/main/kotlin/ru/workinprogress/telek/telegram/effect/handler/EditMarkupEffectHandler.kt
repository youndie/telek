package ru.workinprogress.telek.telegram.effect.handler

import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.entities.ChatId
import ru.workinprogress.telek.EffectFailed
import ru.workinprogress.telek.EffectResult
import ru.workinprogress.telek.telegram.effect.EditMarkupEffect

class EditMarkupEffectHandler : TelegramEffectHandler<EditMarkupEffect> {
    override fun handle(
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
                    EditMarkupEffectResult(effect.chatId, effect.messageId)
                } else {
                    EffectFailed(exception)
                }
            }
}
