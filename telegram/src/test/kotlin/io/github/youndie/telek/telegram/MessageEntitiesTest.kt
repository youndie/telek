package io.github.youndie.telek.telegram

import com.github.kotlintelegrambot.entities.MessageEntity
import io.github.youndie.telek.message
import kotlin.test.Test
import kotlin.test.assertEquals

class MessageEntitiesTest {
    @Test
    fun `plain text produces no entities and keeps markup characters as characters`() {
        val name = "a_b*c[d`e"

        val built = message { text(name) }

        assertEquals(name, built.plain)
        assertEquals(emptyList(), built.asMessageEntities())
    }

    @Test
    fun `a styled run maps to one entity over its own offsets`() {
        val built =
            message {
                text("hi ")
                bold("there")
            }

        assertEquals(
            listOf(
                MessageEntity(
                    MessageEntity.Type.BOLD,
                    offset = 3,
                    length = 5,
                    url = null,
                    user = null,
                    language = null,
                ),
            ),
            built.asMessageEntities(),
        )
    }

    @Test
    fun `every style telegram has survives the mapping`() {
        // The ceiling this release removed: legacy Markdown could express the first two and none of
        // the rest, and the reason was a constant rather than anything Telegram lacked.
        val built =
            message {
                bold("a")
                italic("b")
                underline("c")
                strikethrough("d")
                spoiler("e")
                blockquote { text("f") }
                expandableBlockquote { text("g") }
            }

        assertEquals(
            listOf(
                MessageEntity.Type.BOLD,
                MessageEntity.Type.ITALIC,
                MessageEntity.Type.UNDERLINE,
                MessageEntity.Type.STRIKETHROUGH,
                MessageEntity.Type.SPOILER,
                MessageEntity.Type.BLOCKQUOTE,
                MessageEntity.Type.EXPANDABLE_BLOCKQUOTE,
            ),
            built.asMessageEntities().map { it.type },
        )
    }

    @Test
    fun `a link carries its url and a code block its language`() {
        val built =
            message {
                link(url = "https://example.test", value = "here")
                codeBlock("x", language = "kotlin")
            }

        val entities = built.asMessageEntities()
        assertEquals("https://example.test", entities[0].url)
        assertEquals(MessageEntity.Type.PRE, entities[1].type)
        assertEquals("kotlin", entities[1].language)
    }
}
