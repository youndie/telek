package ru.workinprogress.telek.testing

import ru.workinprogress.telek.State
import ru.workinprogress.telek.TransitionGate
import ru.workinprogress.telek.TransitionResult

/**
 * A [TransitionGate] that applies posted reducers synchronously against an in-memory state,
 * without going through [ru.workinprogress.telek.Telek] or a [ru.workinprogress.telek.UserStateStore].
 *
 * Attach it to a dispatcher under test via `dispatcher.attach(syncTransitionGate)` to observe
 * what `transitionGate.post { ... }` calls would have produced.
 */
class SyncTransitionGate<S : State>(
    private var currentState: S,
    private val onPost: (TransitionResult<S>) -> Unit = {},
) : TransitionGate<S> {
    override fun post(
        chatId: Long,
        reducer: (S) -> TransitionResult<S>,
    ) {
        val result = reducer(currentState)
        currentState = result.newState
        onPost(result)
    }

    fun currentState(): S = currentState
}
