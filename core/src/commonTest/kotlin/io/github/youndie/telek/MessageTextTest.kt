package io.github.youndie.telek

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MessageTextTest {
    @Test
    fun `plain text carries no entities`() {
        val built = message { text("hello") }

        assertEquals("hello", built.plain)
        assertEquals(emptyList(), built.entities())
    }

    @Test
    fun `markup characters in foreign text are not markup`() {
        // The defect this whole model exists to remove: a name somebody typed used to open markup
        // that never closed, and Telegram rejected the entire message. Nothing escapes it here
        // because nothing parses it.
        val name = "a_b*c[d`e"

        val built = message { text(name) }

        assertEquals(name, built.plain)
        assertEquals(emptyList(), built.entities())
    }

    @Test
    fun `a styled run is one entity over its own range`() {
        val built =
            message {
                text("hi ")
                bold("there")
                text("!")
            }

        assertEquals("hi there!", built.plain)
        assertEquals(listOf(TextEntity(TextEntityType.Bold, offset = 3, length = 5)), built.entities())
    }

    @Test
    fun `nesting produces overlapping entities rather than a combined style`() {
        val built =
            message {
                blockquote {
                    text("quoted ")
                    bold("word")
                }
            }

        assertEquals("quoted word", built.plain)
        assertEquals(
            listOf(
                TextEntity(TextEntityType.Blockquote, offset = 0, length = 11),
                TextEntity(TextEntityType.Bold, offset = 7, length = 4),
            ),
            built.entities(),
        )
    }

    @Test
    fun `offsets are utf16 code units so an astral emoji counts twice`() {
        // The one case a length taken in characters gets wrong, and it is invisible in every
        // message that does not contain one.
        val emoji = "😀" // U+1F600, two UTF-16 units
        assertEquals(2, emoji.length)

        val built =
            message {
                text(emoji)
                bold("x")
            }

        assertEquals(listOf(TextEntity(TextEntityType.Bold, offset = 2, length = 1)), built.entities())
    }

    @Test
    fun `an empty styled block produces no entity`() {
        // Telegram rejects a zero-length entity, and an empty bold block is an accident.
        val built =
            message {
                text("a")
                bold { }
            }

        assertEquals("a", built.plain)
        assertEquals(emptyList(), built.entities())
    }

    @Test
    fun `a link keeps its url out of the text`() {
        val built = message { link(url = "https://example.test", value = "here") }

        assertEquals("here", built.plain)
        assertEquals(
            listOf(TextEntity(TextEntityType.TextLink, offset = 0, length = 4, url = "https://example.test")),
            built.entities(),
        )
    }

    @Test
    fun `a code block carries its language and an inline code does not`() {
        val built =
            message {
                code("inline")
                codeBlock("fun main() = Unit", language = "kotlin")
            }

        assertEquals("inlinefun main() = Unit", built.plain)
        assertEquals(
            listOf(
                TextEntity(TextEntityType.Code, offset = 0, length = 6),
                TextEntity(TextEntityType.Pre, offset = 6, length = 17, language = "kotlin"),
            ),
            built.entities(),
        )
    }

    @Test
    fun `row puts its content on its own line without doubling a newline`() {
        val built =
            message {
                text("first\n")
                row { text("second") }
                row { text("third") }
            }

        assertEquals("first\nsecond\nthird\n", built.plain)
    }

    @Test
    fun `list separates items with a blank line`() {
        val built = message { list(listOf("a", "b")) { text(it) } }

        assertEquals("a\n\nb", built.plain)
    }

    @Test
    fun `entities come back ordered by where they open`() {
        val built =
            message {
                bold("one")
                text(" ")
                italic("two")
            }

        val offsets = built.entities().map { it.offset }
        assertTrue(offsets == offsets.sorted(), "expected entities ordered by offset, got $offsets")
    }

    // The layout cases below came from TelegramTextBuilderTest, which existed twice — once per
    // transport — and is now one builder in :core. Ported rather than dropped: the behaviour they
    // describe is the same, and losing a guard while moving its subject is how a move goes wrong.

    @Test
    fun `br appends a single newline`() {
        assertEquals(
            "a\nb",
            message {
                text("a")
                br()
                text("b")
            }.plain,
        )
    }

    @Test
    fun `br2 appends a double newline`() {
        assertEquals(
            "a\n\nb",
            message {
                text("a")
                br2()
                text("b")
            }.plain,
        )
    }

    @Test
    fun `row inserts a newline before and after the block when needed`() {
        assertEquals(
            "intro\nboxed\noutro",
            message {
                text("intro")
                row { text("boxed") }
                text("outro")
            }.plain,
        )
    }

    @Test
    fun `row does not double the newline if already at line start`() {
        assertEquals(
            "intro\nboxed\n",
            message {
                text("intro")
                br()
                row { text("boxed") }
            }.plain,
        )
    }

    @Test
    fun `list separates three items with a blank line and not after the last`() {
        assertEquals(
            "a\n\nb\n\nc",
            message { list(listOf("a", "b", "c")) { item -> text(item) } }.plain,
        )
    }
}
