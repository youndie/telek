package io.github.youndie.telek.docs

import io.github.youndie.telek.FileRef
import io.github.youndie.telek.Input
import io.github.youndie.telek.Photo
import io.github.youndie.telek.State
import io.github.youndie.telek.StateDispatcher
import io.github.youndie.telek.TransitionResult
import io.github.youndie.telek.ktg.sendMessage
import io.github.youndie.telek.noTransition
import io.github.youndie.telek.transition

// Compiles the README's "What an input can be" section.

sealed interface Passport : State {
    data object AwaitingScan : Passport

    data class Received(
        val fileId: String,
    ) : Passport
}

class PassportDispatcher : StateDispatcher<Passport>() {
    override val startCommand: String = "passport"
    override val stateClass = Passport::class

    override fun transition(
        state: Passport,
        input: Input,
    ): TransitionResult<Passport> =
        when {
            state is Passport.AwaitingScan && input is Photo -> {
                transition {
                    newState = Passport.Received(input.file.fileId)
                    sendMessage(input.chatId, "Got it.")
                }
            }

            else -> {
                noTransition(state)
            }
        }
}

/** The README's "declare your own" example. */
data class Voice(
    override val chatId: Long,
    val file: FileRef,
) : Input
