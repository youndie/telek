package io.github.youndie.telek.ktg.effect.handler

import dev.inmo.tgbotapi.bot.TelegramBot
import dev.inmo.tgbotapi.extensions.api.send.sendMessage
import dev.inmo.tgbotapi.types.message.MarkdownParseMode
import dev.inmo.tgbotapi.types.toChatId
import io.github.youndie.telek.EffectResult
import io.github.youndie.telek.ktg.effect.SendMessageEffect

class SendMessageEffectHandler : KtgEffectHandler<SendMessageEffect> {
    override suspend fun handle(
        bot: TelegramBot,
        effect: SendMessageEffect,
    ): EffectResult =
        bot
            .sendMessage(
                chatId = effect.chatId.toChatId(),
                text = effect.text,
                parseMode = MarkdownParseMode,
                replyMarkup = effect.markup,
            ).let { sent ->
                SendMessageEffectResult(effect.chatId, sent.messageId.long)
            }
}
