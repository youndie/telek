package io.github.youndie.telek

import kotlin.reflect.KClass

public abstract class StateDispatcher<T : State> :
    Dispatcher,
    StateMachine<T, Input> {
    protected lateinit var transitionGate: TransitionGate<T>
    public abstract val stateClass: KClass<T>

    public open fun canHandleCallback(data: String): Boolean = startCommand == data

    public fun attach(transitionGate: TransitionGate<T>) {
        this.transitionGate = transitionGate
    }

    public open fun entry(input: Input): TransitionResult<T>? = null

    public fun handle(
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
    public open fun transition(
        state: T,
        event: Event,
    ): TransitionResult<T> = noTransition(state)

    public fun handleEvent(
        current: State,
        event: Event,
    ): TransitionResult<T>? =
        if (stateClass.isInstance(current)) {
            @Suppress("UNCHECKED_CAST")
            transition(current as T, event)
        } else {
            null
        }

    public open fun onEffectResults(
        state: State,
        effectResults: List<EffectResult>,
    ) {
        effectResults.lastOrNull()?.let { onEffectResult(state, it) }
    }

    public open fun onEffectResult(
        state: State,
        effectResult: EffectResult,
    ) {
    }
}

public interface Dispatcher {
    public val startCommand: String
}

public interface StateMachine<S : State, I : Input> {
    public fun transition(
        state: S,
        input: I,
    ): TransitionResult<S>
}
