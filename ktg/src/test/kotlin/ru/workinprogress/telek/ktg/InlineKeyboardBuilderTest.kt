package ru.workinprogress.telek.ktg

import dev.inmo.tgbotapi.types.buttons.InlineKeyboardButtons.CallbackDataInlineKeyboardButton
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardButtons.URLInlineKeyboardButton
import kotlin.test.Test
import kotlin.test.assertEquals

class InlineKeyboardBuilderTest {
    @Test
    fun `row with callback produces a CallbackData button`() {
        val markup = inlineKeyboard { row { callback(text = "Yes", data = "yes") } }

        val button = markup.keyboard.single().single()
        assertEquals(CallbackDataInlineKeyboardButton("Yes", "yes"), button)
    }

    @Test
    fun `row with url produces a Url button`() {
        val markup = inlineKeyboard { row { url(text = "Docs", url = "https://example.com") } }

        val button = markup.keyboard.single().single()
        assertEquals(URLInlineKeyboardButton("Docs", "https://example.com"), button)
    }

    @Test
    fun `multiple rows preserve order`() {
        val markup =
            inlineKeyboard {
                row { callback("First", "1") }
                row { callback("Second", "2") }
            }

        assertEquals(2, markup.keyboard.size)
        assertEquals(CallbackDataInlineKeyboardButton("First", "1"), markup.keyboard[0].single())
        assertEquals(CallbackDataInlineKeyboardButton("Second", "2"), markup.keyboard[1].single())
    }

    @Test
    fun `inlineKeyboard helper matches manual builder usage`() {
        val viaHelper = inlineKeyboard { row { callback("A", "a") } }
        val viaBuilder = InlineKeyboardBuilder().apply { row { callback("A", "a") } }.build()

        assertEquals(viaBuilder, viaHelper)
    }
}
