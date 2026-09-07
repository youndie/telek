package io.github.youndie.telek.support

import io.github.youndie.telek.AsyncEffectHandler
import io.github.youndie.telek.Debounced
import io.github.youndie.telek.Effect
import io.github.youndie.telek.EffectExecutor
import io.github.youndie.telek.EffectHandler
import io.github.youndie.telek.EffectResult
import io.github.youndie.telek.EffectSuccess
import io.github.youndie.telek.Event
import io.github.youndie.telek.ExecutionContext
import io.github.youndie.telek.FinalState
import io.github.youndie.telek.Input
import io.github.youndie.telek.State
import io.github.youndie.telek.StateDispatcher
import io.github.youndie.telek.TelekInterceptor
import io.github.youndie.telek.TransitionResult
import io.github.youndie.telek.noTransition
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
