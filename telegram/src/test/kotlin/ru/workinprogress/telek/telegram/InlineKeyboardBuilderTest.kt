package ru.workinprogress.telek.telegram

import com.github.kotlintelegrambot.entities.keyboard.InlineKeyboardButton
import kotlin.test.Test
import kotlin.test.assertEquals

class InlineKeyboardBuilderTest {
    @Test
    fun `row with callback produces a CallbackData button`() {
        val markup = inlineKeyboard { row { callback(text = "Yes", data = "yes") } }

        val button = markup.inlineKeyboard.single().single()
        assertEquals(InlineKeyboardButton.CallbackData("Yes", "yes"), button)
    }

    @Test
    fun `row with url produces a Url button`() {
        val markup = inlineKeyboard { row { url(text = "Docs", url = "https://example.com") } }

        val button = markup.inlineKeyboard.single().single()
        assertEquals(InlineKeyboardButton.Url("Docs", "https://example.com"), button)
    }

    @Test
    fun `multiple rows preserve order`() {
        val markup =
            inlineKeyboard {
                row { callback("First", "1") }
                row { callback("Second", "2") }
            }

        assertEquals(2, markup.inlineKeyboard.size)
        assertEquals(InlineKeyboardButton.CallbackData("First", "1"), markup.inlineKeyboard[0].single())
        assertEquals(InlineKeyboardButton.CallbackData("Second", "2"), markup.inlineKeyboard[1].single())
    }

    @Test
    fun `inlineKeyboard helper matches manual builder usage`() {
        val viaHelper = inlineKeyboard { row { callback("A", "a") } }
        val viaBuilder = InlineKeyboardBuilder().apply { row { callback("A", "a") } }.build()

        assertEquals(viaBuilder, viaHelper)
    }
}
