package ru.workinprogress.telek

import kotlin.reflect.KClass

abstract class StateDispatcher<T : State> :
    Dispatcher,
    StateMachine<T, Input> {
    protected lateinit var transitionGate: TransitionGate<T>
    abstract val stateClass: KClass<T>

    open fun canHandleCallback(data: String): Boolean = startCommand == data

    fun attach(transitionGate: TransitionGate<T>) {
        this.transitionGate = transitionGate
    }

    open fun entry(input: Input): TransitionResult<T>? = null

    fun handle(
        current: State,
        input: Input,
    ): TransitionResult<T>? =
        entry(input) ?: if (stateClass.isInstance(current)) {
            @Suppress("UNCHECKED_CAST")
            transition(current as T, input)
        } else {
            null
        }

    /**
     * Handles an [Event] — the result of an [AsyncEffectHandler] re-entering the FSM. Defaults to
     * a no-op transition; override for any state that starts async work. There is no `entry`
     * equivalent for events — unlike [Input], an [Event] never starts a flow, only routes by the
     * chat's current state (see [FindDispatcherStrategy]).
     */
    open fun transition(
        state: T,
        event: Event,
    ): TransitionResult<T> = noTransition(state)

    fun handleEvent(
        current: State,
        event: Event,
    ): TransitionResult<T>? =
        if (stateClass.isInstance(current)) {
            @Suppress("UNCHECKED_CAST")
            transition(current as T, event)
        } else {
            null
        }

    open fun onEffectResults(
        state: State,
        effectResults: List<EffectResult>,
    ) {
        effectResults.lastOrNull()?.let { onEffectResult(state, it) }
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

interface StateMachine<S : State, I : Input> {
    fun transition(
        state: S,
        input: I,
    ): TransitionResult<S>
}
