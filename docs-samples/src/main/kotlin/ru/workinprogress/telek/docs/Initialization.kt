// Compiled copy of README.md's "Initialization" section — keep both in sync.
package ru.workinprogress.telek.docs

import com.github.kotlintelegrambot.bot
import com.github.kotlintelegrambot.dispatch
import ru.workinprogress.telek.Telek
import ru.workinprogress.telek.telegram.TelegramContextSource
import ru.workinprogress.telek.telegram.connect
import ru.workinprogress.telek.telegram.effect.telegramEffectExecutor

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
