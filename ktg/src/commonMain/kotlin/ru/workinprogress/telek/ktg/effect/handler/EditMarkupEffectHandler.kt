package ru.workinprogress.telek.ktg.effect.handler

import dev.inmo.tgbotapi.bot.TelegramBot
import dev.inmo.tgbotapi.extensions.api.edit.reply_markup.editMessageReplyMarkup
import dev.inmo.tgbotapi.types.asTelegramMessageId
import dev.inmo.tgbotapi.types.toChatId
import ru.workinprogress.telek.EffectResult
import ru.workinprogress.telek.ktg.effect.EditMarkupEffect

class EditMarkupEffectHandler : KtgEffectHandler<EditMarkupEffect> {
    override suspend fun handle(
        bot: TelegramBot,
        effect: EditMarkupEffect,
    ): EffectResult =
        bot
            .editMessageReplyMarkup(
                chatId = effect.chatId.toChatId(),
                messageId = effect.messageId.asTelegramMessageId(),
                replyMarkup = effect.markup,
            ).let {
                EditMarkupEffectResult(effect.chatId, effect.messageId)
            }
}
