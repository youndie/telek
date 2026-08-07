package ru.workinprogress.telek.ktg

import kotlin.test.Test
import kotlin.test.assertEquals

class TelegramTextBuilderTest {
    @Test
    fun `text appends raw fragments`() {
        val result = telegramMessage { text("hello") }

        assertEquals("hello", result)
    }

    @Test
    fun `bold wraps the value in asterisks`() {
        val result = telegramMessage { bold("hi") }

        assertEquals("*hi*", result)
    }

    @Test
    fun `br appends a single newline`() {
        val result =
            telegramMessage {
                text("a")
                br()
                text("b")
            }

        assertEquals("a\nb", result)
    }

    @Test
    fun `br2 appends a double newline`() {
        val result =
            telegramMessage {
                text("a")
                br2()
                text("b")
            }

        assertEquals("a\n\nb", result)
    }

    @Test
    fun `row inserts a newline before and after the block when needed`() {
        val result =
            telegramMessage {
                text("intro")
                row { text("boxed") }
                text("outro")
            }

        assertEquals("intro\nboxed\noutro", result)
    }

    @Test
    fun `row does not double the newline if already at line start`() {
        val result =
            telegramMessage {
                text("intro")
                br()
                row { text("boxed") }
            }

        assertEquals("intro\nboxed", result)
    }

    @Test
    fun `list separates items with br2 but not after the last item`() {
        val result =
            telegramMessage {
                list(listOf("a", "b", "c")) { item -> text(item) }
            }

        assertEquals("a\n\nb\n\nc", result)
    }

    @Test
    fun `build trims indentation and toString matches build`() {
        val builder = TelegramTextBuilder().apply { text("value") }

        assertEquals(builder.build(), builder.toString())
    }
}
