package ru.workinprogress.telek

import kotlin.reflect.KClass

abstract class StateDispatcher<T : State>(
    private val effectExecutor: EffectExecutor,
) : Dispatcher,
    StateMachine<T, Input> {
    abstract val stateClass: KClass<T>

    open fun entry(input: Input): TransitionResult<T>? = null

    suspend fun handle(
        context: ExecutionContext,
        current: State,
        input: Input,
    ): State {
        entry(input)?.let {
            executeEffects(context, it.effects)
            return it.newState
        }

        if (!stateClass.isInstance(current)) return current
        @Suppress("UNCHECKED_CAST")
        val result = transition(current as T, input)
        executeEffects(context, result.effects)
        return result.newState
    }

    suspend fun executeEffects(
        context: ExecutionContext,
        effects: List<Effect>,
    ) {
        effectExecutor.execute(context, effects)
    }
}

interface Dispatcher {
    val startCommand: String
}

interface StateMachine<S : State, I> {
    fun transition(
        state: S,
        input: I,
    ): TransitionResult<S>
}
