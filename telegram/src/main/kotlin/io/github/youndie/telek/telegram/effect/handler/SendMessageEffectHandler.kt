package io.github.youndie.telek.telegram.effect.handler

import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.entities.ChatId
import io.github.youndie.telek.EffectResult
import io.github.youndie.telek.telegram.asMessageEntities
import io.github.youndie.telek.telegram.effect.SendMessageEffect

public class SendMessageEffectHandler : TelegramEffectHandler<SendMessageEffect> {
    override suspend fun handle(
        bot: Bot,
        effect: SendMessageEffect,
    ): EffectResult =
        bot
            .sendMessage(
                chatId = ChatId.fromId(effect.chatId),
                text = effect.message.plain,
                entities = effect.message.asMessageEntities(),
                replyMarkup = effect.markup,
            ).fold({
                SendMessageEffectResult(effect.chatId, it.messageId)
            }, { error ->
                TelegramEffectError(effect.chatId, error)
            })
}
