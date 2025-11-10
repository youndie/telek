package ru.workinprogress.telek.example

import kotlinx.serialization.Serializable
import ru.workinprogress.telek.FinalState
import ru.workinprogress.telek.State

@Serializable
sealed class ExampleState : State {
    @Serializable
    data object WaitingString : ExampleState()

    @Serializable
    data class SelectingNumber(
        val string: String,
    ) : ExampleState()

    @Serializable
    data class LoadingCatFact(
        val number: Int,
        val string: String,
        val messageId: Long? = null,
    ) : ExampleState()

    @Serializable
    data class Confirming(
        val number: Int,
        val string: String,
        val catFact: String,
    ) : ExampleState()

    @Serializable
    data class Error(
        val errorMessage: String,
    ) : ExampleState(),
        FinalState

    @Serializable
    data object Done : ExampleState(), FinalState
}
