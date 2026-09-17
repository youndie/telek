package io.github.youndie.telek.persistence

import io.github.youndie.telek.ConversationKey
import io.github.youndie.telek.FinalState
import io.github.youndie.telek.State
import kotlinx.serialization.Serializable

/** A chat-only key: its storageId is the bare chatId, so the file names read as they always did. */
fun k(chatId: Long): ConversationKey = ConversationKey.chat(chatId)

@Serializable
sealed interface PersistenceTestState : State {
    @Serializable
    data class Waiting(
        val value: Int = 0,
    ) : PersistenceTestState

    @Serializable
    data class Done(
        val value: Int,
    ) : PersistenceTestState,
        FinalState
}
