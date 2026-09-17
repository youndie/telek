package io.github.youndie.telek.telegram

import com.github.kotlintelegrambot.entities.MessageEntity
import io.github.youndie.telek.MessageText
import io.github.youndie.telek.TextEntityType
import io.github.youndie.telek.entities

/**
 * This message as kotlin-telegram-bot's entities.
 *
 * Unlike `:ktg`, this client takes the Bot API's own shape — plain text plus offsets — so the whole
 * mapping is a rename over [io.github.youndie.telek.entities], which `:core` computes and `:core`
 * tests, on the JVM and on native, without either client library present. The offsets are in UTF-16
 * code units, which is what Telegram counts and what Kotlin already indexes in.
 *
 * `public` for the same reason `:ktg`'s is: a bot reaching past telek for something telek does not
 * model should be able to build the body with the same builder.
 */
public fun MessageText.asMessageEntities(): List<MessageEntity> =
    entities().map { entity ->
        MessageEntity(
            type = entity.type.asKtbType(),
            offset = entity.offset,
            length = entity.length,
            url = entity.url,
            user = null,
            language = entity.language,
        )
    }

private fun TextEntityType.asKtbType(): MessageEntity.Type =
    when (this) {
        TextEntityType.Bold -> MessageEntity.Type.BOLD
        TextEntityType.Italic -> MessageEntity.Type.ITALIC
        TextEntityType.Underline -> MessageEntity.Type.UNDERLINE
        TextEntityType.Strikethrough -> MessageEntity.Type.STRIKETHROUGH
        TextEntityType.Spoiler -> MessageEntity.Type.SPOILER
        TextEntityType.Blockquote -> MessageEntity.Type.BLOCKQUOTE
        TextEntityType.ExpandableBlockquote -> MessageEntity.Type.EXPANDABLE_BLOCKQUOTE
        TextEntityType.Code -> MessageEntity.Type.CODE
        TextEntityType.Pre -> MessageEntity.Type.PRE
        TextEntityType.TextLink -> MessageEntity.Type.TEXT_LINK
    }
