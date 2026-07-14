package ru.workinprogress.telek.telegram

import ru.workinprogress.telek.State
import ru.workinprogress.telek.telegram.effect.EditMarkupEffect
import ru.workinprogress.telek.telegram.effect.EditMessageEffect
import ru.workinprogress.telek.telegram.effect.SendMessageEffect
import ru.workinprogress.telek.transition
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

private data class DummyState(
    val v: Int = 0,
) : State

class TelegramTransitionsTest {
    @Test
    fun `sendMessage with raw text adds a SendMessageEffect`() {
        val result =
            transition<DummyState> {
                newState = DummyState()
                sendMessage(chatId = 1, text = "hi")
            }

        val effect = assertIs<SendMessageEffect>(result.effects.single())
        assertEquals(1, effect.chatId)
        assertEquals("hi", effect.text)
        assertNull(effect.markup)
    }

    @Test
    fun `sendMessage DSL overload builds text and keyboard`() {
        val result =
            transition<DummyState> {
                newState = DummyState()
                sendMessage(
                    chatId = 1,
                    message = { text("Confirm?") },
                    keyboard = { row { callback("Yes", "yes") } },
                )
            }

        val effect = assertIs<SendMessageEffect>(result.effects.single())
        assertEquals("Confirm?", effect.text)
        assertEquals(1, effect.markup?.inlineKeyboard?.size)
    }

    @Test
    fun `sendMessage DSL overload without keyboard has null markup`() {
        val result =
            transition<DummyState> {
                newState = DummyState()
                sendMessage(chatId = 1, message = { text("hi") })
            }

        val effect = assertIs<SendMessageEffect>(result.effects.single())
        assertNull(effect.markup)
    }

    @Test
    fun `editMessage adds an EditMessageEffect`() {
        val result =
            transition<DummyState> {
                newState = DummyState()
                editMessage(chatId = 1, messageId = 2, text = "edited")
            }

        val effect = assertIs<EditMessageEffect>(result.effects.single())
        assertEquals(1, effect.chatId)
        assertEquals(2, effect.messageId)
        assertEquals("edited", effect.text)
    }

    @Test
    fun `editMessage DSL overload builds text and keyboard`() {
        val result =
            transition<DummyState> {
                newState = DummyState()
                editMessage(
                    chatId = 1,
                    messageId = 2,
                    message = { text("edited") },
                    keyboard = { row { callback("Ok", "ok") } },
                )
            }

        val effect = assertIs<EditMessageEffect>(result.effects.single())
        assertEquals("edited", effect.text)
        assertEquals(1, effect.markup?.inlineKeyboard?.size)
    }

    @Test
    fun `editMarkup adds an EditMarkupEffect that can clear the keyboard`() {
        val result =
            transition<DummyState> {
                newState = DummyState()
                editMarkup(chatId = 1, messageId = 2, markup = null)
            }

        val effect = assertIs<EditMarkupEffect>(result.effects.single())
        assertNull(effect.markup)
    }
}
