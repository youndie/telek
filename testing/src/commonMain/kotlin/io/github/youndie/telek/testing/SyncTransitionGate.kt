package io.github.youndie.telek.testing

import io.github.youndie.telek.State
import io.github.youndie.telek.TransitionGate
import io.github.youndie.telek.TransitionResult

/**
 * A [TransitionGate] that applies posted reducers synchronously against an in-memory state,
 * without going through [io.github.youndie.telek.Telek] or a [io.github.youndie.telek.UserStateStore].
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
