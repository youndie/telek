package ru.workinprogress.telek.example

import com.github.kotlintelegrambot.bot
import com.github.kotlintelegrambot.dispatch
import ru.workinprogress.telek.Telek
import ru.workinprogress.telek.telegram.TelegramContextSource
import ru.workinprogress.telek.telegram.connect
import ru.workinprogress.telek.telegram.effect.defaultEffectRegistry
import ru.workinprogress.telek.telegram.effect.telegramEffectExecutor

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
