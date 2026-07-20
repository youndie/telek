package ru.workinprogress.telek.telegram

import com.github.kotlintelegrambot.Bot
import kotlinx.coroutines.CompletableDeferred

/**
 * Resolves the [TelegramContext] lazily, once a [Bot] instance becomes available.
 *
 * `bot { }` only hands out its [Bot] instance inside each `dispatch { }` handler (message,
 * callbackQuery, ...) — it isn't available yet while the bot is being configured. Share one
 * instance between [telegramEffectExecutor] and [connect] so the executor can await the same bot
 * the dispatcher observes, without [ru.workinprogress.telek.Telek] needing to own or track it.
 */
class TelegramContextSource {
    private val botDeferred = CompletableDeferred<Bot>()

    internal fun provide(bot: Bot) {
        botDeferred.complete(bot)
    }

    suspend fun context(): TelegramContext = TelegramContext(botDeferred.await())
}
