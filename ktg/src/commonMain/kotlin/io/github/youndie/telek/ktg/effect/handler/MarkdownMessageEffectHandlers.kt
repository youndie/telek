@file:Suppress("DEPRECATION")

package io.github.youndie.telek.ktg.effect.handler

import dev.inmo.tgbotapi.bot.TelegramBot
import dev.inmo.tgbotapi.extensions.api.edit.text.editMessageText
import dev.inmo.tgbotapi.extensions.api.send.sendMessage
import dev.inmo.tgbotapi.types.asTelegramMessageId
import dev.inmo.tgbotapi.types.message.MarkdownParseMode
import dev.inmo.tgbotapi.types.toChatId
import io.github.youndie.telek.EffectResult
import io.github.youndie.telek.ktg.effect.EditMarkdownMessageEffect
import io.github.youndie.telek.ktg.effect.SendMarkdownMessageEffect

/**
 * The pre-entities path, kept for one release so an upgrading bot's Markdown keeps rendering.
 *
 * This is the only place left in `:ktg` that names a parse mode, and when the deprecated effects go
 * this file goes with them.
 */
public class SendMarkdownMessageEffectHandler : KtgEffectHandler<SendMarkdownMessageEffect> {
    override suspend fun handle(
        bot: TelegramBot,
        effect: SendMarkdownMessageEffect,
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

/** The edit half of [SendMarkdownMessageEffectHandler]. */
public class EditMarkdownMessageEffectHandler : KtgEffectHandler<EditMarkdownMessageEffect> {
    override suspend fun handle(
        bot: TelegramBot,
        effect: EditMarkdownMessageEffect,
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
