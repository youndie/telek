package ru.workinprogress.telek.ktg

import dev.inmo.tgbotapi.bot.TelegramBot
import kotlinx.coroutines.CompletableDeferred

/**
 * Resolves the [KtgContext] lazily, once a [TelegramBot] instance becomes available.
 *
 * Unlike kotlin-telegram-bot, ktgbotapi hands out the [TelegramBot] instance up front
 * (`telegramBot(token)`), so the usual wiring is simply `KtgContextSource(bot)` — the deferred
 * shape is kept for the cases where the executor has to be built before the bot exists (and to
 * mirror `:telegram`'s `TelegramContextSource`). Share one instance between [ktgEffectExecutor]
 * and [connect] so the executor awaits the same bot the behaviour context observes, without
 * [ru.workinprogress.telek.Telek] needing to own or track it.
 */
class KtgContextSource(
    bot: TelegramBot? = null,
) {
    private val botDeferred = CompletableDeferred<TelegramBot>()

    init {
        if (bot != null) provide(bot)
    }

    /** Idempotent — the first bot wins, later calls are ignored. Called by [connect]. */
    fun provide(bot: TelegramBot) {
        botDeferred.complete(bot)
    }

    suspend fun context(): KtgContext = KtgContext(botDeferred.await())
}
