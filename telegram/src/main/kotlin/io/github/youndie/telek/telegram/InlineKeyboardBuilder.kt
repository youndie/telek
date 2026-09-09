package io.github.youndie.telek.telegram

import com.github.kotlintelegrambot.entities.InlineKeyboardMarkup
import com.github.kotlintelegrambot.entities.keyboard.InlineKeyboardButton

@DslMarker
public annotation class InlineKeyboardMarkupDsl

@InlineKeyboardMarkupDsl
public class InlineKeyboardBuilder {
    private val rows = mutableListOf<List<InlineKeyboardButton>>()

    public fun row(block: RowBuilder.() -> Unit) {
        rows += RowBuilder().apply(block).build()
    }

    public fun build(): InlineKeyboardMarkup = InlineKeyboardMarkup.create(rows)
}

@InlineKeyboardMarkupDsl
public class RowBuilder {
    private val buttons = mutableListOf<InlineKeyboardButton>()

    public fun callback(
        text: String,
        data: String,
    ) {
        buttons += InlineKeyboardButton.CallbackData(text, data)
    }

    public fun url(
        text: String,
        url: String,
    ) {
        buttons += InlineKeyboardButton.Url(text, url)
    }

    public fun build(): List<InlineKeyboardButton> = buttons
}

public fun inlineKeyboard(block: InlineKeyboardBuilder.() -> Unit): InlineKeyboardMarkup =
    InlineKeyboardBuilder().apply(block).build()
