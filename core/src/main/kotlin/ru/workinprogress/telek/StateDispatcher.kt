package ru.workinprogress.telek

import kotlin.reflect.KClass

abstract class StateDispatcher<T : State> :
    Dispatcher,
    StateMachine<T, Input> {
    protected lateinit var transitionGate: TransitionGate<T>
    abstract val stateClass: KClass<T>

    fun attach(transitionGate: TransitionGate<T>) {
        this.transitionGate = transitionGate
    }

    open fun entry(input: Input): TransitionResult<T>? = null

    fun handle(
        current: State,
        input: Input,
    ): TransitionResult<T>? {
        if (!stateClass.isInstance(current)) return null
        @Suppress("UNCHECKED_CAST")
        return entry(input) ?: transition(current as T, input)
    }

    open fun onEffectResult(
        state: State,
        effectResult: EffectResult,
    ) {
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
