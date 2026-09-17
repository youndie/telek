@file:Suppress("DEPRECATION")

package io.github.youndie.telek.telegram.effect.handler

import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.ParseMode
import io.github.youndie.telek.EffectFailed
import io.github.youndie.telek.EffectResult
import io.github.youndie.telek.telegram.effect.EditMarkdownMessageEffect
import io.github.youndie.telek.telegram.effect.SendMarkdownMessageEffect

/**
 * The pre-entities path, kept for one release so an upgrading bot's Markdown keeps rendering.
 *
 * The only place left in `:telegram` that names a parse mode; this file goes when the deprecated
 * effects do.
 */
public class SendMarkdownMessageEffectHandler : TelegramEffectHandler<SendMarkdownMessageEffect> {
    override suspend fun handle(
        bot: Bot,
        effect: SendMarkdownMessageEffect,
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

/** The edit half of [SendMarkdownMessageEffectHandler]. */
public class EditMarkdownMessageEffectHandler : TelegramEffectHandler<EditMarkdownMessageEffect> {
    override suspend fun handle(
        bot: Bot,
        effect: EditMarkdownMessageEffect,
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
