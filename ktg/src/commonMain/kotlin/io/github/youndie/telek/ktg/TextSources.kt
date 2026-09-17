package io.github.youndie.telek.ktg

import dev.inmo.tgbotapi.types.message.textsources.TextSource
import dev.inmo.tgbotapi.types.message.textsources.blockquoteTextSource
import dev.inmo.tgbotapi.types.message.textsources.boldTextSource
import dev.inmo.tgbotapi.types.message.textsources.codeTextSource
import dev.inmo.tgbotapi.types.message.textsources.expandableBlockquoteTextSource
import dev.inmo.tgbotapi.types.message.textsources.italicTextSource
import dev.inmo.tgbotapi.types.message.textsources.linkTextSource
import dev.inmo.tgbotapi.types.message.textsources.preTextSource
import dev.inmo.tgbotapi.types.message.textsources.regularTextSource
import dev.inmo.tgbotapi.types.message.textsources.spoilerTextSource
import dev.inmo.tgbotapi.types.message.textsources.strikethroughTextSource
import dev.inmo.tgbotapi.types.message.textsources.underlineTextSource
import io.github.youndie.telek.MessageText
import io.github.youndie.telek.TextPiece
import io.github.youndie.telek.TextStyle

/**
 * This message as ktgbotapi's own text sources.
 *
 * The mapping is the whole cost of keeping [MessageText] in `:core` instead of putting ktgbotapi's
 * types in telek's signatures — thirty lines, in one place, that nobody writing a bot ever reads.
 * What it buys is that `:telegram` maps the same document to its own client's entities, one builder
 * serves both, and telek's public API keeps naming Telegram rather than a library.
 *
 * `public` because a bot that reaches past telek for something it does not model — a media caption,
 * a reply to a specific message — should be able to build the text with the same builder and hand
 * it to ktgbotapi directly, instead of dropping back to a string.
 */
public fun MessageText.asTextSources(): List<TextSource> = pieces.map { it.asTextSource() }

private fun TextPiece.asTextSource(): TextSource =
    when (this) {
        is TextPiece.Plain -> {
            regularTextSource(text)
        }

        is TextPiece.Code -> {
            codeTextSource(text)
        }

        is TextPiece.CodeBlock -> {
            preTextSource(text, language)
        }

        is TextPiece.Link -> {
            linkTextSource(text = MessageText(text).plain, url = url)
        }

        is TextPiece.Styled -> {
            val children = text.map { it.asTextSource() }
            when (style) {
                TextStyle.Bold -> boldTextSource(children)
                TextStyle.Italic -> italicTextSource(children)
                TextStyle.Underline -> underlineTextSource(children)
                TextStyle.Strikethrough -> strikethroughTextSource(children)
                TextStyle.Spoiler -> spoilerTextSource(children)
                TextStyle.Blockquote -> blockquoteTextSource(children)
                TextStyle.ExpandableBlockquote -> expandableBlockquoteTextSource(children)
            }
        }
    }
