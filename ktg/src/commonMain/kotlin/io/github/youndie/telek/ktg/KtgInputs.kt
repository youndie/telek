package io.github.youndie.telek.ktg

import dev.inmo.tgbotapi.abstracts.OptionallyFromUser
import dev.inmo.tgbotapi.types.files.TelegramMediaFile
import dev.inmo.tgbotapi.types.message.abstracts.ContentMessage
import dev.inmo.tgbotapi.types.message.content.ContactContent
import dev.inmo.tgbotapi.types.message.content.DocumentContent
import dev.inmo.tgbotapi.types.message.content.LocationContent
import dev.inmo.tgbotapi.types.message.content.PhotoContent
import dev.inmo.tgbotapi.types.message.content.TextContent
import dev.inmo.tgbotapi.types.queries.callback.AbstractMessageCallbackQuery
import dev.inmo.tgbotapi.types.queries.callback.DataCallbackQuery
import io.github.youndie.telek.Callback
import io.github.youndie.telek.Contact
import io.github.youndie.telek.Document
import io.github.youndie.telek.FileRef
import io.github.youndie.telek.Location
import io.github.youndie.telek.Message
import io.github.youndie.telek.Photo
import dev.inmo.tgbotapi.types.message.abstracts.Message as KtgMessage

/**
 * ktgbotapi models ids as value classes (`ChatId`/`RawChatId`, `MessageId`); telek keys everything
 * by a plain `Long`. These adapters are the whole of that translation — they're public so a bot
 * wiring its own update handling (webhooks, a custom `FlowsUpdatesFilter`) can reuse them instead
 * of going through [connect].
 */
public val KtgMessage.telekChatId: Long
    get() = chat.id.chatId.long

/**
 * Who sent it, `null` when nothing did that telek can name — a channel post, an automatic forward.
 * A [Keying][io.github.youndie.telek.Keying] that wants a per-person key falls back to the chat in
 * that case; see [io.github.youndie.telek.ConversationKey].
 *
 * A [dev.inmo.tgbotapi.types.chat.User] IS a chat in ktgbotapi's model (`User : PrivateChat`), so
 * its id unwraps through exactly the same value classes as [telekChatId] does.
 */
public val KtgMessage.telekUserId: Long?
    get() =
        (this as? OptionallyFromUser)
            ?.from
            ?.id
            ?.chatId
            ?.long

public fun ContentMessage<TextContent>.asTelekInput(): Message =
    Message(
        chatId = telekChatId,
        text = content.text,
    )

/**
 * `null` for a callback query that has no message attached to it (an inline-mode one), which telek
 * can't key by `chatId` and therefore can't route.
 */
public fun DataCallbackQuery.asTelekInput(): Callback? {
    val message = (this as? AbstractMessageCallbackQuery)?.message ?: return null
    return Callback(
        chatId = message.telekChatId,
        messageId = message.messageId.long,
        data = data,
    )
}

/**
 * ktgbotapi wraps each id and size in its own value class (`FileId`, `FileUniqueId`, `FileSize`);
 * [FileRef] carries the plain values a dispatcher can hold, compare and store.
 */
public fun TelegramMediaFile.asTelekFileRef(): FileRef =
    FileRef(
        fileId = fileId.fileId,
        uniqueId = fileUniqueId.string,
        // ktgbotapi models the size as a ULong; telek's surface stays Long, which no real
        // Telegram file can overflow (the API's own upload ceiling is 2 GB).
        sizeBytes = fileSize?.bytes?.toLong(),
    )

/**
 * Carries `content.media` — the size ktgbotapi designates for the collection — and not a pick of
 * telek's own out of `mediaCollection`. telek does not model photo sizes: a wizard step keeps the
 * photo, and a bot that wants a specific size asks the transport for it.
 */
public fun ContentMessage<PhotoContent>.asTelekInput(): Photo =
    Photo(
        chatId = telekChatId,
        messageId = messageId.long,
        file = content.media.asTelekFileRef(),
        caption = content.text,
    )

public fun ContentMessage<DocumentContent>.asTelekInput(): Document =
    Document(
        chatId = telekChatId,
        messageId = messageId.long,
        file = content.media.asTelekFileRef(),
        fileName = content.media.fileName,
        mimeType = content.media.mimeType?.raw,
        caption = content.text,
    )

public fun ContentMessage<ContactContent>.asTelekInput(): Contact =
    Contact(
        chatId = telekChatId,
        messageId = messageId.long,
        phoneNumber = content.contact.phoneNumber,
        firstName = content.contact.firstName,
        lastName = content.contact.lastName,
        userId =
            content.contact.userId
                ?.chatId
                ?.long,
    )

/**
 * Latitude and longitude only, and a live location collapses to the point it was at. telek has no
 * notion of a location that keeps moving — a wizard step that asks "where are you" is answered by a
 * point, and anything richer is a bot's own [io.github.youndie.telek.Input].
 */
public fun ContentMessage<LocationContent>.asTelekInput(): Location =
    Location(
        chatId = telekChatId,
        messageId = messageId.long,
        latitude = content.location.latitude,
        longitude = content.location.longitude,
    )
