// Compiled copy of README.md's "Usage with Telegram bot" section — keep both in sync.
package ru.workinprogress.telek.docs

import ru.workinprogress.telek.Callback
import ru.workinprogress.telek.Input
import ru.workinprogress.telek.Message
import ru.workinprogress.telek.State
import ru.workinprogress.telek.StateDispatcher
import ru.workinprogress.telek.TransitionResult
import ru.workinprogress.telek.noTransition
import ru.workinprogress.telek.telegram.editMarkup
import ru.workinprogress.telek.telegram.sendMessage
import ru.workinprogress.telek.transition

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
                    // Move to Confirming state, keep number, save input string
                    newState =
                        ExampleState.Confirming(
                            number = state.number,
                            string = input.text,
                        )
                    // Send confirmation message with inline keyboard (Confirm/Cancel)
                    sendMessage(
                        input.chatId,
                        message = {
                            row {
                                text("Confirm?")
                            }
                        },
                        keyboard = {
                            row {
                                callback(text = "Confirm", data = "example_confirm")
                                callback(text = "Cancel", data = "example_cancel")
                            }
                        },
                    )
                }
            }

            // If in Confirming state and receive a callback from the inline keyboard
            is ExampleState.Confirming if (input is Callback) -> {
                transition {
                    // Move to Done state
                    newState = ExampleState.Done
                    // Remove inline keyboard from message
                    editMarkup(input.chatId, input.messageId, null)
                    // Respond with confirmation or cancellation based on callback data
                    if (input.data.contains("example_confirm")) {
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
