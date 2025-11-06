# telek

**type-safe kotlin toolkit** for building **Telegram bots**, **wizard-flows**, and other **interactive systems** powered by **FSM**

> 🧩 state + input → newState + effects

---

```kotlin 
//usage with telegram-bot:
class ExampleDispatcher(
    effectExecutor: EffectExecutor,
) : StateDispatcher<ExampleState>(effectExecutor) {
    override val startCommand = "example"
    override val stateClass = ExampleState::class

    override fun transition(
        state: ExampleState,
        input: Input,
    ): TransitionResult<ExampleState> =
        when (state) {
            is ExampleState.WaitingString if (input is Input.Message) -> {
                transition {
                    newState =
                        ExampleState.Confirming(
                            number = state.number,
                            string = input.text,
                        )

                    sendMessage(
                        input.chatId,
                        message = {
                            row {
                                text("CONFIRM?")
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

            is ExampleState.Confirming if (input is Input.Callback) -> {
                transition {
                    newState = ExampleState.Done
                    
                    editMarkup(input.chatId, input.messageId, null)

                    if (input.data.contains("example_confirm")) {
                        sendMessage(input.chatId, "confirmed")
                    } else {
                        sendMessage(input.chatId, "canceled")
                    }
                }
            }

            else -> noTransition(state)
        }
}
