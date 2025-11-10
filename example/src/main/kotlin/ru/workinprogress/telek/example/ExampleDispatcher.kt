package ru.workinprogress.telek.example

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import ru.workinprogress.telek.EffectResult
import ru.workinprogress.telek.Input
import ru.workinprogress.telek.State
import ru.workinprogress.telek.StateDispatcher
import ru.workinprogress.telek.TransitionResult
import ru.workinprogress.telek.noTransition
import ru.workinprogress.telek.router.callback
import ru.workinprogress.telek.router.routes
import ru.workinprogress.telek.router.tryDecode
import ru.workinprogress.telek.telegram.editMarkup
import ru.workinprogress.telek.telegram.editMessage
import ru.workinprogress.telek.telegram.effect.handler.SendMessageEffectResult
import ru.workinprogress.telek.telegram.inlineKeyboard
import ru.workinprogress.telek.telegram.sendMessage

class ExampleDispatcher(
    private val networkUseCase: ExampleNetworkUseCase,
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Default),
) : StateDispatcher<ExampleState>() {
    override val startCommand = "example"
    override val stateClass = ExampleState::class

    private val routeRegistry =
        routes {
            register<ExampleRouteSelect>()
            register<ExampleRouteConfirm>()
            register<ExampleRouteCancel>()
        }

    override fun entry(input: Input): TransitionResult<ExampleState>? =
        if (input is Input.Message && input.text == "/$startCommand") {
            ru.workinprogress.telek.transition {
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
            is ExampleState.WaitingString if (input is Input.Message) -> {
                ru.workinprogress.telek.transition {
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
                input is Input.Callback &&
                    routeRegistry.typeIs<ExampleRouteSelect>(input.data)
            ) -> {
                val numberValue =
                    input.tryDecode<ExampleRouteSelect>(routeRegistry)?.number ?: return noTransition(state)

                coroutineScope.launch {
                    networkUseCase().fold({ catFact ->
                        transitionGate.post(input.chatId) { currentState ->
                            val loadingMessageId = (currentState as? ExampleState.LoadingCatFact)?.messageId

                            ru.workinprogress.telek.transition {
                                newState =
                                    ExampleState.Confirming(
                                        number = numberValue,
                                        string = state.string,
                                        catFact = catFact.fact,
                                    )

                                loadingMessageId?.let { messageId ->
                                    editMessage(input.chatId, messageId, "Cat fact loaded")
                                }

                                sendMessage(
                                    input.chatId,
                                    catFact.fact,
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
                    }, { error ->
                        transitionGate.post(input.chatId) { currentState ->
                            ru.workinprogress.telek.transition {
                                newState =
                                    ExampleState.Error(
                                        errorMessage = error.message ?: "Unknown error",
                                    )
                            }
                        }
                    })
                }

                ru.workinprogress.telek.transition {
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
                }
            }

            is ExampleState.Confirming if (input is Input.Callback) -> {
                when {
                    routeRegistry.typeIs<ExampleRouteConfirm>(input.data) -> {
                        ru.workinprogress.telek.transition {
                            newState = ExampleState.Done
                            editMarkup(input.chatId, input.messageId, markup = null)
                            sendMessage(input.chatId, "Confirmed")
                        }
                    }

                    routeRegistry.typeIs<ExampleRouteCancel>(input.data) -> {
                        ru.workinprogress.telek.transition {
                            newState = ExampleState.Done
                            editMarkup(input.chatId, input.messageId, markup = null)
                            sendMessage(input.chatId, "Canceled")
                        }
                    }

                    else -> noTransition(state)
                }
            }

            else -> noTransition(state)
        }

    override fun onEffectResult(
        state: State,
        effectResult: EffectResult,
    ) {
        if (state is ExampleState.LoadingCatFact && effectResult is SendMessageEffectResult) {
            transitionGate.post(effectResult.chatId) { currentState ->
                if (currentState is ExampleState.LoadingCatFact) {
                    ru.workinprogress.telek.transition {
                        newState = currentState.copy(messageId = effectResult.messageId)
                    }
                } else {
                    noTransition(state)
                }
            }
        }
    }
}
