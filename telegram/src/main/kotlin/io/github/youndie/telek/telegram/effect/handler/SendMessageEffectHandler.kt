package io.github.youndie.telek.telegram.effect.handler

import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.ParseMode
import io.github.youndie.telek.EffectResult
import io.github.youndie.telek.telegram.effect.SendMessageEffect

class SendMessageEffectHandler : TelegramEffectHandler<SendMessageEffect> {
    override suspend fun handle(
        bot: Bot,
        effect: SendMessageEffect,
    ): EffectResult =
        bot
            .sendMessage(
                chatId = ChatId.fromId(effect.chatId),
                text = effect.text,
                parseMode = ParseMode.MARKDOWN,
                replyMarkup = effect.markup,
            ).fold({
                SendMessageEffectResult(effect.chatId, it.messageId)
            }, { error ->
                TelegramEffectError(effect.chatId, error)
            })
}
