package io.github.youndie.telek.ktg

import dev.inmo.tgbotapi.types.message.textsources.BoldTextSource
import dev.inmo.tgbotapi.types.message.textsources.CodeTextSource
import dev.inmo.tgbotapi.types.message.textsources.ExpandableBlockquoteTextSource
import dev.inmo.tgbotapi.types.message.textsources.PreTextSource
import dev.inmo.tgbotapi.types.message.textsources.RegularTextSource
import dev.inmo.tgbotapi.types.message.textsources.TextLinkTextSource
import io.github.youndie.telek.message
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class TextSourcesTest {
    @Test
    fun `plain text becomes a regular source and keeps markup characters as characters`() {
        val name = "a_b*c[d`e"

        val sources = message { text(name) }.asTextSources()

        assertEquals(name, assertIs<RegularTextSource>(sources.single()).source)
    }

    @Test
    fun `a styled run becomes the matching source`() {
        val sources = message { bold("hi") }.asTextSources()

        val bold = assertIs<BoldTextSource>(sources.single())
        assertEquals("hi", bold.subsources.joinToString("") { it.source })
    }

    @Test
    fun `nesting survives the mapping`() {
        val sources = message { expandableBlockquote { bold("inner") } }.asTextSources()

        val quote = assertIs<ExpandableBlockquoteTextSource>(sources.single())
        assertIs<BoldTextSource>(quote.subsources.single())
    }

    @Test
    fun `a link carries its url and a code block its language`() {
        val sources =
            message {
                link(url = "https://example.test", value = "here")
                codeBlock("x", language = "kotlin")
                code("y")
            }.asTextSources()

        assertEquals("https://example.test", assertIs<TextLinkTextSource>(sources[0]).url)
        assertEquals("kotlin", assertIs<PreTextSource>(sources[1]).language)
        assertEquals("y", assertIs<CodeTextSource>(sources[2]).source)
    }
}
