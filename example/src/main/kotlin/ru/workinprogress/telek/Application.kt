package ru.workinprogress.telek

import com.github.kotlintelegrambot.bot
import com.github.kotlintelegrambot.dispatch
import ru.workinprogress.telek.example.ExampleDispatcher
import ru.workinprogress.telek.example.ExampleNetworkUseCase
import ru.workinprogress.telek.telegram.connect
import ru.workinprogress.telek.telegram.effect.telegramEffectExecutor

fun main() {
    val exampleDispatcher = ExampleDispatcher(ExampleNetworkUseCase())

    val telek =
        Telek(
            dispatchers = listOf(exampleDispatcher),
            effectExecutor = telegramEffectExecutor(),
        )

    val bot =
        bot {
            token = "<BOT-TOKEN>"

            dispatch {
                connect(telek)
            }
        }

    bot.startPolling()
}
