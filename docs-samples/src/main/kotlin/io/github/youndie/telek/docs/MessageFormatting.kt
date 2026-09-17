// Compiled copy of README.md's "Formatting a message" section — keep both in sync.
package io.github.youndie.telek.docs

import io.github.youndie.telek.MessageText
import io.github.youndie.telek.entities
import io.github.youndie.telek.message

fun messageFormattingSample(customerName: String): MessageText =
    message {
        bold("Order confirmed")
        br2()
        // Whatever the person typed. No escaping, at any call site, ever.
        text("Thanks, ")
        text(customerName)
        text("!")
        br2()
        blockquote { text("Delivery on Friday") }
        br()
        spoiler("There is a free sticker in the box")
        br2()
        link(url = "https://example.test/orders", value = "Track it")
        br()
        code("ORD-4711")
        expandableBlockquote {
            text("Full item list")
            br()
            list(listOf("Cheese", "Bread")) { item -> text(item) }
        }
    }

// What a test sees: the text without markup, and the ranges over it.
fun messageFormattingProjections(body: MessageText): Pair<String, Int> = body.plain to body.entities().size
