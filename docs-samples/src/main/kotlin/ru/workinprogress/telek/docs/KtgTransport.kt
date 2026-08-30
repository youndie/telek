// Compiled copy of README.md's "Using ktgbotapi instead" section — keep both in sync.
package ru.workinprogress.telek.docs

import dev.inmo.tgbotapi.bot.ktor.telegramBot
import dev.inmo.tgbotapi.extensions.behaviour_builder.buildBehaviourWithLongPolling
import kotlinx.coroutines.runBlocking
import ru.workinprogress.telek.Callback
import ru.workinprogress.telek.Input
import ru.workinprogress.telek.Message
import ru.workinprogress.telek.StateDispatcher
import ru.workinprogress.telek.Telek
import ru.workinprogress.telek.TransitionResult
import ru.workinprogress.telek.ktg.KtgContextSource
import ru.workinprogress.telek.ktg.connect
import ru.workinprogress.telek.ktg.editMarkup
import ru.workinprogress.telek.ktg.effect.ktgEffectExecutor
import ru.workinprogress.telek.ktg.sendMessage
import ru.workinprogress.telek.noTransition
import ru.workinprogress.telek.transition

// Identical to ExampleDispatcher, except the effect DSL comes from `ru.workinprogress.telek.ktg`
class KtgExampleDispatcher : StateDispatcher<ExampleState>() {
    override val startCommand = "example"

    override val stateClass = ExampleState::class

    override fun transition(
        state: ExampleState,
        input: Input,
    ): TransitionResult<ExampleState> =
        when (state) {
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
                                callback(text = "Confirm", data = "example_confirm")
                                callback(text = "Cancel", data = "example_cancel")
                            }
                        },
                    )
                }
            }

            is ExampleState.Confirming if (input is Callback) -> {
                transition {
                    newState = ExampleState.Done
                    editMarkup(input.chatId, input.messageId, null)
                    sendMessage(input.chatId, "confirmed")
                }
            }

            else -> {
                noTransition(state)
            }
        }
}

fun ktgInitializationSample() =
    runBlocking {
        val bot = telegramBot("telegram token")
        val contextSource = KtgContextSource(bot)

        val telek =
            Telek(
                dispatchers = listOf(KtgExampleDispatcher()),
                effectExecutor = ktgEffectExecutor(contextSource),
            )

        bot
            .buildBehaviourWithLongPolling {
                connect(telek, contextSource)
            }.join()
    }
