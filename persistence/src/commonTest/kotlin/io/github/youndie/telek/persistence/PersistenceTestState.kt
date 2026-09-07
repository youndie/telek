package io.github.youndie.telek.persistence

import io.github.youndie.telek.FinalState
import io.github.youndie.telek.State
import kotlinx.serialization.Serializable

@Serializable
sealed interface PersistenceTestState : State {
    @Serializable
    data class Waiting(
        val value: Int = 0,
    ) : PersistenceTestState

    @Serializable
    data class Done(
        val value: Int,
    ) : PersistenceTestState,
        FinalState
}
