package io.github.youndie.telek.ktg.effect

import dev.inmo.tgbotapi.types.buttons.InlineKeyboardMarkup
import io.github.youndie.telek.Effect
import io.github.youndie.telek.MessageText

public interface KtgEffect : Effect

public data class SendMessageEffect(
    val chatId: Long,
    val message: MessageText,
    val markup: InlineKeyboardMarkup? = null,
) : KtgEffect

public data class EditMessageEffect(
    val chatId: Long,
    val messageId: Long,
    val message: MessageText,
    val markup: InlineKeyboardMarkup? = null,
) : KtgEffect

public data class EditMarkupEffect(
    val chatId: Long,
    val messageId: Long,
    val markup: InlineKeyboardMarkup? = null,
) : KtgEffect

/**
 * A message sent as a legacy-Markdown string, the way every message was sent before entities.
 *
 * A separate effect rather than a flag, because it is a different request: `parse_mode` asks
 * Telegram to find markup inside the text, and entities tell it where the markup is. Keeping them
 * apart means the deprecation is a type that disappears, not a branch somebody forgets to delete —
 * and means a bot that still sends hand-written Markdown keeps rendering exactly as it did while
 * it migrates, instead of having its asterisks silently become literal.
 *
 * Whatever is in [text] is still parsed by Telegram, so foreign text in it still has to be escaped,
 * and unbalanced markup still fails the whole message. That is the defect [MessageText] removes.
 */
@Deprecated(
    "Sends a string for Telegram to parse; foreign text in it must be escaped and can fail the " +
        "whole message. Build the body with `message { }` and use SendMessageEffect.",
    level = DeprecationLevel.WARNING,
)
public data class SendMarkdownMessageEffect(
    val chatId: Long,
    val text: String,
    val markup: InlineKeyboardMarkup? = null,
) : KtgEffect

/** The edit half of [SendMarkdownMessageEffect]; the same caveats apply. */
@Deprecated(
    "Sends a string for Telegram to parse; foreign text in it must be escaped and can fail the " +
        "whole message. Build the body with `message { }` and use EditMessageEffect.",
    level = DeprecationLevel.WARNING,
)
public data class EditMarkdownMessageEffect(
    val chatId: Long,
    val messageId: Long,
    val text: String,
    val markup: InlineKeyboardMarkup? = null,
) : KtgEffect
