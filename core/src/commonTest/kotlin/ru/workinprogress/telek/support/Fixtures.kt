package ru.workinprogress.telek.support

import ru.workinprogress.telek.AsyncEffectHandler
import ru.workinprogress.telek.Debounced
import ru.workinprogress.telek.Effect
import ru.workinprogress.telek.EffectExecutor
import ru.workinprogress.telek.EffectHandler
import ru.workinprogress.telek.EffectResult
import ru.workinprogress.telek.EffectSuccess
import ru.workinprogress.telek.Event
import ru.workinprogress.telek.ExecutionContext
import ru.workinprogress.telek.FinalState
import ru.workinprogress.telek.Input
import ru.workinprogress.telek.State
import ru.workinprogress.telek.StateDispatcher
import ru.workinprogress.telek.TelekInterceptor
import ru.workinprogress.telek.TransitionResult
import ru.workinprogress.telek.noTransition
import kotlin.reflect.KClass

sealed interface TestState : State {
    data class Waiting(
        val value: Int = 0,
    ) : TestState

    data class Confirming(
        val value: Int,
    ) : TestState

    data class Done(
        val value: Int,
    ) : TestState,
        FinalState
}

data class OtherState(
    val n: Int = 0,
) : State

data class TestEffect(
    val tag: String,
) : Effect

data class TestDebouncedEffect(
    val tag: String,
    override val debounceKey: Any,
) : Effect,
    Debounced

data class TestEvent(
    override val chatId: Long,
    val tag: String,
) : Event

open class SimpleDispatcher<T : State>(
    override val startCommand: String,
    override val stateClass: KClass<T>,
    private val transitionFn: (T, Input) -> TransitionResult<T> = { state, _ -> noTransition(state) },
) : StateDispatcher<T>() {
    override fun transition(
        state: T,
        input: Input,
    ): TransitionResult<T> = transitionFn(state, input)
}

object TestExecutionContext : ExecutionContext

class RecordingEffectHandler(
    private val result: (TestEffect) -> EffectResult = { EffectSuccess },
) : EffectHandler<TestEffect> {
    val handled = mutableListOf<TestEffect>()

    override suspend fun handle(
        context: ExecutionContext,
        effect: TestEffect,
    ): EffectResult {
        handled += effect
        return result(effect)
    }
}

class RecordingAsyncEffectHandler(
    private val result: (TestEffect) -> Event? = { null },
) : AsyncEffectHandler<TestEffect> {
    val handled = mutableListOf<TestEffect>()

    override suspend fun handle(
        context: ExecutionContext,
        effect: TestEffect,
    ): Event? {
        handled += effect
        return result(effect)
    }
}

class FakeEffectExecutor(
    private val asyncWorkFor: (Effect) -> (suspend () -> Event?)? = { null },
    private val resultsFor: (Effect) -> EffectResult = { EffectSuccess },
) : EffectExecutor {
    val executed = mutableListOf<List<Effect>>()

    override suspend fun execute(
        effects: List<Effect>,
        dispatchAsync: (key: Any?, work: suspend () -> Event?) -> Unit,
    ): List<EffectResult> {
        executed += effects
        val results = mutableListOf<EffectResult>()
        for (effect in effects) {
            val asyncWork = asyncWorkFor(effect)
            if (asyncWork != null) {
                dispatchAsync((effect as? Debounced)?.debounceKey, asyncWork)
                continue
            }
            results += resultsFor(effect)
        }
        return results
    }
}

class RecordingInterceptor : TelekInterceptor {
    data class BeforeInputCall(
        val chatId: Long,
        val input: Input,
    )

    data class AfterStateChangedCall(
        val chatId: Long,
        val oldState: State?,
        val newState: State,
    )

    data class ErrorCall(
        val chatId: Long,
        val input: Input?,
        val error: Throwable,
    )

    val beforeInput = mutableListOf<BeforeInputCall>()
    val afterStateChanged = mutableListOf<AfterStateChangedCall>()
    val errors = mutableListOf<ErrorCall>()

    override fun onBeforeInput(
        chatId: Long,
        input: Input,
    ) {
        beforeInput += BeforeInputCall(chatId, input)
    }

    override fun onAfterStateChanged(
        chatId: Long,
        oldState: State?,
        newState: State,
    ) {
        afterStateChanged += AfterStateChangedCall(chatId, oldState, newState)
    }

    override fun onError(
        chatId: Long,
        input: Input?,
        error: Throwable,
    ) {
        errors += ErrorCall(chatId, input, error)
    }
}
