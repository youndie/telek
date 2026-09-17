// Compiled copy of README.md's "The same on kotlin-telegram-bot" section — keep both in sync.
//
// The dispatcher is the same object the ktg wiring uses; only this file's imports differ. :telegram
// is in maintenance (see the README's non-goals), which is why it is a note rather than the worked
// example -- and why this sample exists at all: a maintained module still has to compile.
package io.github.youndie.telek.docs

import com.github.kotlintelegrambot.bot
import com.github.kotlintelegrambot.dispatch
import io.github.youndie.telek.Keying
import io.github.youndie.telek.Telek
import io.github.youndie.telek.telegram.TelegramContextSource
import io.github.youndie.telek.telegram.connect
import io.github.youndie.telek.telegram.effect.telegramEffectExecutor

fun telegramInitializationSample() {
    val contextSource = TelegramContextSource()

    val telek =
        Telek(
            dispatchers = listOf(ExampleDispatcher()),
            effectExecutor = telegramEffectExecutor(contextSource),
        )

    bot {
        token = "telegram token"

        dispatch { connect(telek, contextSource, Keying.PerUserInChat) }
    }
}
