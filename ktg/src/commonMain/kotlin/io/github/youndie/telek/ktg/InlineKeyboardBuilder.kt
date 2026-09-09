package io.github.youndie.telek.ktg

import dev.inmo.tgbotapi.types.buttons.InlineKeyboardButtons.CallbackDataInlineKeyboardButton
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardButtons.InlineKeyboardButton
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardButtons.URLInlineKeyboardButton
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardMarkup

@DslMarker
public annotation class InlineKeyboardMarkupDsl

@InlineKeyboardMarkupDsl
public class InlineKeyboardBuilder {
    private val rows = mutableListOf<List<InlineKeyboardButton>>()

    public fun row(block: RowBuilder.() -> Unit) {
        rows += RowBuilder().apply(block).build()
    }

    public fun build(): InlineKeyboardMarkup = InlineKeyboardMarkup(rows)
}

@InlineKeyboardMarkupDsl
public class RowBuilder {
    private val buttons = mutableListOf<InlineKeyboardButton>()

    public fun callback(
        text: String,
        data: String,
    ) {
        buttons += CallbackDataInlineKeyboardButton(text, data)
    }

    public fun url(
        text: String,
        url: String,
    ) {
        buttons += URLInlineKeyboardButton(text, url)
    }

    public fun build(): List<InlineKeyboardButton> = buttons
}

public fun inlineKeyboard(block: InlineKeyboardBuilder.() -> Unit): InlineKeyboardMarkup =
    InlineKeyboardBuilder().apply(block).build()
