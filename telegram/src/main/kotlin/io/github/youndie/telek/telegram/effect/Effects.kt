package io.github.youndie.telek.telegram.effect

import com.github.kotlintelegrambot.entities.InlineKeyboardMarkup
import io.github.youndie.telek.Effect
import io.github.youndie.telek.MessageText

public interface TelegramEffect : Effect

public data class SendMessageEffect(
    val chatId: Long,
    val message: MessageText,
    val markup: InlineKeyboardMarkup? = null,
) : TelegramEffect

public data class EditMessageEffect(
    val chatId: Long,
    val messageId: Long,
    val message: MessageText,
    val markup: InlineKeyboardMarkup? = null,
) : TelegramEffect

public data class EditMarkupEffect(
    val chatId: Long,
    val messageId: Long,
    val markup: InlineKeyboardMarkup? = null,
) : TelegramEffect

/**
 * A message sent as a legacy-Markdown string, the way every message was sent before entities.
 *
 * A separate effect rather than a flag, because it is a different request: `parse_mode` asks
 * Telegram to find markup inside the text, and entities tell it where the markup is. Keeping them
 * apart means the deprecation is a type that disappears rather than a branch somebody forgets, and
 * a bot still sending hand-written Markdown renders exactly as it did while it migrates.
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
) : TelegramEffect

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
) : TelegramEffect
