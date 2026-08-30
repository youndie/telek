package ru.workinprogress.telek.ktg

import dev.inmo.tgbotapi.types.buttons.InlineKeyboardButtons.CallbackDataInlineKeyboardButton
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardButtons.InlineKeyboardButton
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardButtons.URLInlineKeyboardButton
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardMarkup

@DslMarker
annotation class InlineKeyboardMarkupDsl

@InlineKeyboardMarkupDsl
class InlineKeyboardBuilder {
    private val rows = mutableListOf<List<InlineKeyboardButton>>()

    fun row(block: RowBuilder.() -> Unit) {
        rows += RowBuilder().apply(block).build()
    }

    fun build(): InlineKeyboardMarkup = InlineKeyboardMarkup(rows)
}

@InlineKeyboardMarkupDsl
class RowBuilder {
    private val buttons = mutableListOf<InlineKeyboardButton>()

    fun callback(
        text: String,
        data: String,
    ) {
        buttons += CallbackDataInlineKeyboardButton(text, data)
    }

    fun url(
        text: String,
        url: String,
    ) {
        buttons += URLInlineKeyboardButton(text, url)
    }

    fun build(): List<InlineKeyboardButton> = buttons
}

fun inlineKeyboard(block: InlineKeyboardBuilder.() -> Unit): InlineKeyboardMarkup =
    InlineKeyboardBuilder().apply(block).build()
