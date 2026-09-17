// Compiled copy of README.md's "Usage with Telegram bot" section — keep both in sync.
package io.github.youndie.telek.docs

import io.github.youndie.telek.Callback
import io.github.youndie.telek.Input
import io.github.youndie.telek.Message
import io.github.youndie.telek.State
import io.github.youndie.telek.StateDispatcher
import io.github.youndie.telek.TransitionResult
import io.github.youndie.telek.ktg.editMarkup
import io.github.youndie.telek.ktg.sendMessage
import io.github.youndie.telek.noTransition
import io.github.youndie.telek.router.Route
import io.github.youndie.telek.router.RouteContext
import io.github.youndie.telek.router.callback
import io.github.youndie.telek.router.isRouteOf
import io.github.youndie.telek.router.routes
import io.github.youndie.telek.transition
import kotlinx.serialization.Serializable

sealed class ExampleState : State {
    data class WaitingString(
        val number: Int,
    ) : ExampleState()

    data class Confirming(
        val number: Int,
        val string: String,
    ) : ExampleState()

    data object Done : ExampleState()
}

// The two buttons this flow can produce. A route is a type, so the compiler is what keeps the
// button and the branch that handles it in step -- rename one and the other stops compiling.
@RouteContext(scope = "example", action = "confirm")
@Serializable
class ExampleConfirm : Route

@RouteContext(scope = "example", action = "cancel")
@Serializable
class ExampleCancel : Route

val exampleRoutes =
    routes {
        register<ExampleConfirm>()
        register<ExampleCancel>()
    }

// Dispatcher that manages the conversation flow (FSM) for the 'example' command
class ExampleDispatcher : StateDispatcher<ExampleState>() {
    // The command that starts this dispatcher flow
    override val startCommand = "example"

    // The associated state class for this flow
    override val stateClass = ExampleState::class

    // Handles finite-state transitions based on current state and input
    override fun transition(
        state: ExampleState,
        input: Input,
    ): TransitionResult<ExampleState> =
        when (state) {
            // If waiting for a string, and receive a message input from user
            is ExampleState.WaitingString if (input is Message) -> {
                transition {
                    newState =
                        ExampleState.Confirming(
                            number = state.number,
                            string = input.text,
                        )
                    sendMessage(
                        input.chatId,
                        message = { row { text("Confirm?") } },
                        keyboard = {
                            row {
                                callback(name = "Confirm", route = ExampleConfirm())
                                callback(name = "Cancel", route = ExampleCancel())
                            }
                        },
                    )
                }
            }

            // If in Confirming state and receive a callback from the inline keyboard
            is ExampleState.Confirming if (input is Callback) -> {
                transition {
                    newState = ExampleState.Done
                    editMarkup(input.chatId, input.messageId, null)
                    if (input.isRouteOf<ExampleConfirm>(exampleRoutes)) {
                        sendMessage(input.chatId, "confirmed")
                    } else {
                        sendMessage(input.chatId, "canceled")
                    }
                }
            }

            // For all other cases, no state transition
            else -> {
                noTransition(state)
            }
        }
}
