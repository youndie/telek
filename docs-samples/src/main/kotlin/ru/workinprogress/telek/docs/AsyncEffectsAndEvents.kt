// Compiled copy of README.md's "Async effects & Events" section — keep both in sync.
package ru.workinprogress.telek.docs

import ru.workinprogress.telek.AsyncEffectHandler
import ru.workinprogress.telek.Debounced
import ru.workinprogress.telek.Effect
import ru.workinprogress.telek.Event
import ru.workinprogress.telek.ExecutionContext
import ru.workinprogress.telek.Input
import ru.workinprogress.telek.State
import ru.workinprogress.telek.StateDispatcher
import ru.workinprogress.telek.TransitionResult
import ru.workinprogress.telek.noTransition
import ru.workinprogress.telek.telegram.effect.defaultEffectRegistry
import ru.workinprogress.telek.telegram.sendMessage
import ru.workinprogress.telek.transition

// The effect just carries what the handler needs
data class FetchCatFactEffect(
    val chatId: Long,
) : Effect

// ...and what comes back, once it's done
data class CatFactLoaded(
    override val chatId: Long,
    val fact: String,
) : Event

data class CatFactLoadFailed(
    override val chatId: Long,
    val errorMessage: String,
) : Event

data class CatFact(
    val text: String,
)

class FetchCatFactUseCase {
    suspend operator fun invoke(): Result<CatFact> = TODO("network call")
}

// AsyncEffectHandler, not EffectHandler — returns an Event instead of an EffectResult
class FetchCatFactEffectHandler(
    private val networkUseCase: FetchCatFactUseCase,
) : AsyncEffectHandler<FetchCatFactEffect> {
    override suspend fun handle(
        context: ExecutionContext,
        effect: FetchCatFactEffect,
    ): Event =
        networkUseCase()
            .fold(
                { fact -> CatFactLoaded(effect.chatId, fact.text) },
                { error -> CatFactLoadFailed(effect.chatId, error.message ?: "Unknown error") },
            )
}

sealed class MyState : State {
    data object Loading : MyState()

    data class Done(
        val fact: String,
    ) : MyState()

    data class Error(
        val errorMessage: String,
    ) : MyState()
}

fun asyncEffectRegistrationSample(useCase: FetchCatFactUseCase) {
    defaultEffectRegistry().apply {
        registerAsync(FetchCatFactEffect::class, FetchCatFactEffectHandler(useCase))
    }
}

fun asyncEffectUsageSample(input: Input): TransitionResult<MyState> =
    transition {
        newState = MyState.Loading
        sendMessage(input.chatId, "Loading...")
        add(FetchCatFactEffect(chatId = input.chatId)) // fire-and-forget from here on
    }

data class SearchProductsEffect(
    val chatId: Long,
    val query: String,
) : Effect,
    Debounced {
    override val debounceKey: Any get() = "search" // per-chat: same key cancels the previous in-flight search
}

class MyDispatcher : StateDispatcher<MyState>() {
    override val startCommand = "my"
    override val stateClass = MyState::class

    override fun transition(
        state: MyState,
        input: Input,
    ): TransitionResult<MyState> = noTransition(state)

    override fun transition(
        state: MyState,
        event: Event,
    ): TransitionResult<MyState> =
        when {
            state is MyState.Loading && event is CatFactLoaded ->
                transition { newState = MyState.Done(event.fact) }

            state is MyState.Loading && event is CatFactLoadFailed ->
                transition { newState = MyState.Error(event.errorMessage) }

            else -> noTransition(state)
        }
}
