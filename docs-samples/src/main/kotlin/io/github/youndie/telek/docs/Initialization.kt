// Compiled copy of README.md's "Initialization" section — keep both in sync.
package io.github.youndie.telek.docs

import com.github.kotlintelegrambot.bot
import com.github.kotlintelegrambot.dispatch
import io.github.youndie.telek.Telek
import io.github.youndie.telek.telegram.TelegramContextSource
import io.github.youndie.telek.telegram.connect
import io.github.youndie.telek.telegram.effect.telegramEffectExecutor

fun initializationSample() {
    val contextSource = TelegramContextSource()

    val telek =
        Telek(
            dispatchers = listOf(ExampleDispatcher()),
            effectExecutor = telegramEffectExecutor(contextSource),
        )

    bot {
        token = "telegram token"

        dispatch { connect(telek, contextSource) }
    }
}
