// Compiled copy of README.md's "Using ktgbotapi instead" section — keep both in sync.
package io.github.youndie.telek.docs

import dev.inmo.tgbotapi.bot.ktor.telegramBot
import dev.inmo.tgbotapi.extensions.behaviour_builder.buildBehaviourWithLongPolling
import io.github.youndie.telek.Callback
import io.github.youndie.telek.Input
import io.github.youndie.telek.Message
import io.github.youndie.telek.StateDispatcher
import io.github.youndie.telek.Telek
import io.github.youndie.telek.TransitionResult
import io.github.youndie.telek.ktg.KtgContextSource
import io.github.youndie.telek.ktg.connect
import io.github.youndie.telek.ktg.editMarkup
import io.github.youndie.telek.ktg.effect.ktgEffectExecutor
import io.github.youndie.telek.ktg.sendMessage
import io.github.youndie.telek.noTransition
import io.github.youndie.telek.transition
import kotlinx.coroutines.runBlocking

// Identical to ExampleDispatcher, except the effect DSL comes from `io.github.youndie.telek.ktg`
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
