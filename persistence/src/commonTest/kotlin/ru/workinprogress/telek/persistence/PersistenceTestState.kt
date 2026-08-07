package ru.workinprogress.telek.persistence

import kotlinx.serialization.Serializable
import ru.workinprogress.telek.FinalState
import ru.workinprogress.telek.State

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
