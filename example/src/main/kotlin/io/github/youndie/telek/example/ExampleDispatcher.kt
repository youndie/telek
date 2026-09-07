package io.github.youndie.telek.example

import io.github.youndie.telek.Callback
import io.github.youndie.telek.Event
import io.github.youndie.telek.Input
import io.github.youndie.telek.Message
import io.github.youndie.telek.StateDispatcher
import io.github.youndie.telek.TransitionResult
import io.github.youndie.telek.noTransition
import io.github.youndie.telek.router.callback
import io.github.youndie.telek.router.routes
import io.github.youndie.telek.router.tryDecode
import io.github.youndie.telek.telegram.editMarkup
import io.github.youndie.telek.telegram.editMessage
import io.github.youndie.telek.telegram.inlineKeyboard
import io.github.youndie.telek.telegram.sendMessage
import io.github.youndie.telek.transition

class ExampleDispatcher : StateDispatcher<ExampleState>() {
    override val startCommand = "example"
    override val stateClass = ExampleState::class

    private val routeRegistry =
        routes {
            register<ExampleRouteSelect>()
            register<ExampleRouteConfirm>()
            register<ExampleRouteCancel>()
        }

    override fun entry(input: Input): TransitionResult<ExampleState>? =
        if (input is Message && input.text == "/$startCommand") {
            transition {
                newState = ExampleState.WaitingString

                sendMessage(
                    input.chatId,
                    message = {
                        row { text("Enter string") }
                    },
                )
            }
        } else {
            null
        }

    override fun transition(
        state: ExampleState,
        input: Input,
    ): TransitionResult<ExampleState> =
        when (state) {
            is ExampleState.WaitingString if (input is Message) -> {
                transition {
                    newState =
                        ExampleState.SelectingNumber(
                            string = input.text,
                        )

                    sendMessage(
                        input.chatId,
                        message = {
                            row {
                                text("Select number")
                            }
                        },
                        keyboard = {
                            row {
                                callback("1", ExampleRouteSelect(1))
                                callback("2", ExampleRouteSelect(2))
                                callback("3", ExampleRouteSelect(3))
                            }
                        },
                    )
                }
            }

            is ExampleState.SelectingNumber if (
                input is Callback &&
                    routeRegistry.typeIs<ExampleRouteSelect>(input.data)
            ) -> {
                val numberValue =
                    input.tryDecode<ExampleRouteSelect>(routeRegistry)?.number ?: return noTransition(state)

                transition {
                    newState =
                        ExampleState.LoadingCatFact(
                            number = numberValue,
                            string = state.string,
                        )

                    editMessage(
                        input.chatId,
                        input.messageId,
                        "Selected number: $numberValue, string: ${state.string}",
                        markup = null,
                    )

                    sendMessage(
                        input.chatId,
                        message = {
                            row {
                                text("Loading cat fact...")
                            }
                        },
                    )

                    // Async effect: fetches over the network without blocking this transition.
                    // Its result re-enters as an Event, handled by the transition(state, event)
                    // overload below — no manual coroutine, no transitionGate.post.
                    add(FetchCatFactEffect(chatId = input.chatId, number = numberValue, string = state.string))
                }
            }

            is ExampleState.Confirming if (input is Callback) -> {
                when {
                    routeRegistry.typeIs<ExampleRouteConfirm>(input.data) -> {
                        transition {
                            newState = ExampleState.Done
                            editMarkup(input.chatId, input.messageId, markup = null)
                            sendMessage(input.chatId, "Confirmed")
                        }
                    }

                    routeRegistry.typeIs<ExampleRouteCancel>(input.data) -> {
                        transition {
                            newState = ExampleState.Done
                            editMarkup(input.chatId, input.messageId, markup = null)
                            sendMessage(input.chatId, "Canceled")
                        }
                    }

                    else -> {
                        noTransition(state)
                    }
                }
            }

            else -> {
                noTransition(state)
            }
        }

    override fun transition(
        state: ExampleState,
        event: Event,
    ): TransitionResult<ExampleState> =
        when {
            state is ExampleState.LoadingCatFact && event is CatFactLoaded -> {
                transition {
                    newState =
                        ExampleState.Confirming(
                            number = event.number,
                            string = event.string,
                            catFact = event.fact,
                        )

                    sendMessage(
                        event.chatId,
                        event.fact,
                        markup =
                            inlineKeyboard {
                                row {
                                    callback("Confirm", ExampleRouteConfirm())
                                    callback("Cancel", ExampleRouteCancel())
                                }
                            },
                    )
                }
            }

            state is ExampleState.LoadingCatFact && event is CatFactLoadFailed -> {
                transition {
                    newState = ExampleState.Error(errorMessage = event.errorMessage)
                }
            }

            else -> {
                noTransition(state)
            }
        }
}
