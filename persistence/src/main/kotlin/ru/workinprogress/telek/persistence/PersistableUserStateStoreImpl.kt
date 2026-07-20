package ru.workinprogress.telek.persistence

import ru.workinprogress.telek.FinalState
import ru.workinprogress.telek.State
import ru.workinprogress.telek.UpdateResult
import ru.workinprogress.telek.UserStateStore
import java.util.concurrent.ConcurrentHashMap

interface StateStorage<S : State> {
    suspend fun save(
        chatId: Long,
        state: S,
    )

    suspend fun load(chatId: Long): S?

    suspend fun delete(chatId: Long)
}

/**
 * See [UserStateStore]'s contract note: [Telek][ru.workinprogress.telek.Telek] never calls
 * [update] concurrently for the same `chatId`, so this store does no locking of its own — the
 * in-memory cache below is a plain [ConcurrentHashMap], safe for concurrent access across
 * *different* chatIds without any additional synchronization.
 */
class PersistableUserStateStoreImpl<T : State>(
    val stateStorage: FileStateStorage<T>,
) : UserStateStore {
    private val states = ConcurrentHashMap<Long, State>()

    override suspend fun get(chatId: Long): State? =
        states[chatId] ?: run {
            val loaded = stateStorage.load(chatId)
            if (loaded != null) {
                states[chatId] = loaded
            }
            loaded
        }

    override suspend fun update(
        chatId: Long,
        block: suspend (State?) -> UpdateResult,
    ): UpdateResult {
        val current =
            states[chatId] ?: run {
                val loaded = stateStorage.load(chatId)
                if (loaded != null) states[chatId] = loaded
                loaded
            }

        val updateResult = block(current)

        if (updateResult.newState is FinalState) {
            stateStorage.delete(chatId)
            states.remove(chatId)
        } else {
            states[chatId] = updateResult.newState
            @Suppress("UNCHECKED_CAST")
            stateStorage.save(chatId, updateResult.newState as T)
        }

        return updateResult
    }

    override suspend fun clear(chatId: Long) {
        stateStorage.delete(chatId)
        states.remove(chatId)
    }
}
