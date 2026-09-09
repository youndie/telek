package io.github.youndie.telek.telegram.effect.handler

import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.ParseMode
import io.github.youndie.telek.EffectFailed
import io.github.youndie.telek.EffectResult
import io.github.youndie.telek.telegram.effect.EditMessageEffect

public class EditMessageEffectHandler : TelegramEffectHandler<EditMessageEffect> {
    override suspend fun handle(
        bot: Bot,
        effect: EditMessageEffect,
    ): EffectResult =
        bot
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
