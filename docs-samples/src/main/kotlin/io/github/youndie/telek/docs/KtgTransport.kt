// Compiled copy of README.md's "Initialization" section — keep both in sync.
package io.github.youndie.telek.docs

import dev.inmo.tgbotapi.bot.ktor.telegramBot
import dev.inmo.tgbotapi.extensions.behaviour_builder.buildBehaviourWithLongPolling
import io.github.youndie.telek.Telek
import io.github.youndie.telek.ktg.KtgContextSource
import io.github.youndie.telek.ktg.connect
import io.github.youndie.telek.ktg.effect.ktgEffectExecutor
import kotlinx.coroutines.runBlocking

fun ktgInitializationSample() =
    runBlocking {
        val bot = telegramBot("telegram token")
        val contextSource = KtgContextSource(bot)

        val telek =
            Telek(
                dispatchers = listOf(ExampleDispatcher()),
                effectExecutor = ktgEffectExecutor(contextSource),
            )

        bot
            .buildBehaviourWithLongPolling {
                connect(telek, contextSource)
            }.join()
    }
