package ru.workinprogress.telek.example

import ru.workinprogress.telek.AsyncEffectHandler
import ru.workinprogress.telek.Effect
import ru.workinprogress.telek.Event
import ru.workinprogress.telek.ExecutionContext

/** Async effect: fetch a cat fact over the network without blocking the transition that starts it. */
data class FetchCatFactEffect(
    val chatId: Long,
    val number: Int,
    val string: String,
) : Effect

data class CatFactLoaded(
    override val chatId: Long,
    val number: Int,
    val string: String,
    val fact: String,
) : Event

data class CatFactLoadFailed(
    override val chatId: Long,
    val errorMessage: String,
) : Event

/**
 * Doesn't need Telegram context — a plain [AsyncEffectHandler] works for any async effect whose
 * result doesn't itself require the bot (contrast with `TelegramAsyncEffectHandler`).
 */
class FetchCatFactEffectHandler(
    private val networkUseCase: ExampleNetworkUseCase,
) : AsyncEffectHandler<FetchCatFactEffect> {
    override suspend fun handle(
        context: ExecutionContext,
        effect: FetchCatFactEffect,
    ): Event =
        networkUseCase().fold({ catFact ->
            CatFactLoaded(chatId = effect.chatId, number = effect.number, string = effect.string, fact = catFact.fact)
        }, { error ->
            CatFactLoadFailed(chatId = effect.chatId, errorMessage = error.message ?: "Unknown error")
        })
}
