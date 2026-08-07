package ru.workinprogress.telek.ktg

import dev.inmo.tgbotapi.types.message.abstracts.ContentMessage
import dev.inmo.tgbotapi.types.message.content.TextContent
import dev.inmo.tgbotapi.types.queries.callback.AbstractMessageCallbackQuery
import dev.inmo.tgbotapi.types.queries.callback.DataCallbackQuery
import ru.workinprogress.telek.Callback
import ru.workinprogress.telek.Message
import dev.inmo.tgbotapi.types.message.abstracts.Message as KtgMessage

/**
 * ktgbotapi models ids as value classes (`ChatId`/`RawChatId`, `MessageId`); telek keys everything
 * by a plain `Long`. These adapters are the whole of that translation — they're public so a bot
 * wiring its own update handling (webhooks, a custom `FlowsUpdatesFilter`) can reuse them instead
 * of going through [connect].
 */
val KtgMessage.telekChatId: Long
    get() = chat.id.chatId.long

fun ContentMessage<TextContent>.asTelekInput(): Message =
    Message(
        chatId = telekChatId,
        text = content.text,
    )

/**
 * `null` for a callback query that has no message attached to it (an inline-mode one), which telek
 * can't key by `chatId` and therefore can't route.
 */
fun DataCallbackQuery.asTelekInput(): Callback? {
    val message = (this as? AbstractMessageCallbackQuery)?.message ?: return null
    return Callback(
        chatId = message.telekChatId,
        messageId = message.messageId.long,
        data = data,
    )
}
