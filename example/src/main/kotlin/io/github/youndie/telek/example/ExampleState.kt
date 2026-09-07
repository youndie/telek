package io.github.youndie.telek.example

import io.github.youndie.telek.FinalState
import io.github.youndie.telek.State
import kotlinx.serialization.Serializable

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
