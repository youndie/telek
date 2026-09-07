package io.github.youndie.telek.example

import com.github.kotlintelegrambot.bot
import com.github.kotlintelegrambot.dispatch
import io.github.youndie.telek.Telek
import io.github.youndie.telek.telegram.TelegramContextSource
import io.github.youndie.telek.telegram.connect
import io.github.youndie.telek.telegram.effect.defaultEffectRegistry
import io.github.youndie.telek.telegram.effect.telegramEffectExecutor

fun main() {
    val exampleDispatcher = ExampleDispatcher()
    val contextSource = TelegramContextSource()
    val effectRegistry =
        defaultEffectRegistry().apply {
            registerAsync(FetchCatFactEffect::class, FetchCatFactEffectHandler(ExampleNetworkUseCase()))
        }

    val telek =
        Telek(
            dispatchers = listOf(exampleDispatcher),
            effectExecutor = telegramEffectExecutor(contextSource, effectRegistry),
        )

    val bot =
        bot {
            token = "<BOT-TOKEN>"

            dispatch {
                connect(telek, contextSource)
            }
        }

    bot.startPolling()
}
