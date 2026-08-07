package ru.workinprogress.telek.ktg.effect.handler

import dev.inmo.tgbotapi.bot.TelegramBot
import dev.inmo.tgbotapi.extensions.api.edit.text.editMessageText
import dev.inmo.tgbotapi.types.asTelegramMessageId
import dev.inmo.tgbotapi.types.message.MarkdownParseMode
import dev.inmo.tgbotapi.types.toChatId
import ru.workinprogress.telek.EffectResult
import ru.workinprogress.telek.ktg.effect.EditMessageEffect

class EditMessageEffectHandler : KtgEffectHandler<EditMessageEffect> {
    override suspend fun handle(
        bot: TelegramBot,
        effect: EditMessageEffect,
    ): EffectResult =
        bot
            .editMessageText(
                chatId = effect.chatId.toChatId(),
                messageId = effect.messageId.asTelegramMessageId(),
                text = effect.text,
                parseMode = MarkdownParseMode,
                replyMarkup = effect.markup,
            ).let {
                EditMessageEffectResult(effect.chatId, effect.messageId)
            }
}
