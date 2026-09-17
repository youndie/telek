package io.github.youndie.telek.ktg

import io.github.youndie.telek.MessageText
import io.github.youndie.telek.State
import io.github.youndie.telek.ktg.effect.EditMarkupEffect
import io.github.youndie.telek.ktg.effect.EditMessageEffect
import io.github.youndie.telek.ktg.effect.SendMarkdownMessageEffect
import io.github.youndie.telek.ktg.effect.SendMessageEffect
import io.github.youndie.telek.transition
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

private data class DummyState(
    val v: Int = 0,
) : State

class KtgTransitionsTest {
    @Test
    fun `sendMessage with a built body adds a SendMessageEffect`() {
        val result =
            transition<DummyState> {
                newState = DummyState()
                sendMessage(chatId = 1, MessageText.plain("hi"))
            }

        val effect = assertIs<SendMessageEffect>(result.effects.single())
        assertEquals(1, effect.chatId)
        assertEquals("hi", effect.message.plain)
        assertNull(effect.markup)
    }

    @Test
    @Suppress("DEPRECATION")
    fun `the deprecated string overload still goes out as legacy markdown`() {
        // Kept for one release on purpose: had this started sending the string literally, every
        // asterisk in an upgrading bot's messages would have become text, and nothing would fail.
        val result =
            transition<DummyState> {
                newState = DummyState()
                sendMessage(chatId = 1, text = "*hi*")
            }

        val effect = assertIs<SendMarkdownMessageEffect>(result.effects.single())
        assertEquals("*hi*", effect.text)
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
        assertEquals("Confirm?", effect.message.plain)
        assertEquals(1, effect.markup?.keyboard?.size)
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
                editMessage(chatId = 1, messageId = 2, MessageText.plain("edited"))
            }

        val effect = assertIs<EditMessageEffect>(result.effects.single())
        assertEquals(1, effect.chatId)
        assertEquals(2, effect.messageId)
        assertEquals("edited", effect.message.plain)
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
        assertEquals("edited", effect.message.plain)
        assertEquals(1, effect.markup?.keyboard?.size)
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
