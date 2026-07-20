package ru.workinprogress.telek

import com.github.kotlintelegrambot.bot
import com.github.kotlintelegrambot.dispatch
import ru.workinprogress.telek.example.ExampleDispatcher
import ru.workinprogress.telek.example.ExampleNetworkUseCase
import ru.workinprogress.telek.example.FetchCatFactEffect
import ru.workinprogress.telek.example.FetchCatFactEffectHandler
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
