// Compiled copy of README.md's "State that outlives a flow" section — keep both in sync.
package io.github.youndie.telek.docs

import io.github.youndie.telek.ConversationKey
import io.github.youndie.telek.FinalState
import io.github.youndie.telek.State
import io.github.youndie.telek.UpdateResult
import io.github.youndie.telek.UserStateStore
import kotlinx.serialization.Serializable

// Your row. The flow's state is one field of it; the rest outlives the flow.
@Serializable
data class Profile(
    val flow: State? = null,
    val language: String = "en",
    val menuMessageId: Long? = null,
)

class ProfileStore(
    private val rows: MutableMap<Long, Profile> = mutableMapOf(),
) : UserStateStore {
    override suspend fun get(key: ConversationKey): State? = rows[key.chatId]?.flow

    override suspend fun update(
        key: ConversationKey,
        block: suspend (State?) -> UpdateResult,
    ): UpdateResult {
        val row = rows[key.chatId] ?: Profile()
        val result = block(row.flow)
        // A FinalState ends the flow, not the person: clear the field, keep the row.
        rows[key.chatId] = row.copy(flow = result.newState.takeUnless { it is FinalState })
        return result
    }

    override suspend fun clear(key: ConversationKey) {
        rows[key.chatId]?.let { rows[key.chatId] = it.copy(flow = null) }
    }

    // The language survives every wizard the user ever finishes.
    fun languageOf(chatId: Long): String = rows[chatId]?.language ?: "en"
}
