package ru.workinprogress.telek.telegram

import com.github.kotlintelegrambot.entities.InlineKeyboardMarkup
import com.github.kotlintelegrambot.entities.keyboard.InlineKeyboardButton

@DslMarker
annotation class InlineKeyboardMarkupDsl

@InlineKeyboardMarkupDsl
class InlineKeyboardBuilder {
    private val rows = mutableListOf<List<InlineKeyboardButton>>()

    fun row(block: RowBuilder.() -> Unit) {
        rows += RowBuilder().apply(block).build()
    }

    fun build(): InlineKeyboardMarkup = InlineKeyboardMarkup.create(rows)
}

@InlineKeyboardMarkupDsl
class RowBuilder {
    private val buttons = mutableListOf<InlineKeyboardButton>()

    fun callback(
        text: String,
        data: String,
    ) {
        buttons += InlineKeyboardButton.CallbackData(text, data)
    }

    fun url(
        text: String,
        url: String,
    ) {
        buttons += InlineKeyboardButton.Url(text, url)
    }

    fun build(): List<InlineKeyboardButton> = buttons
}

fun inlineKeyboard(block: InlineKeyboardBuilder.() -> Unit): InlineKeyboardMarkup = InlineKeyboardBuilder().apply(block).build()
