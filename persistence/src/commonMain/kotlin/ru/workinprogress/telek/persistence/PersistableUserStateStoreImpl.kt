package ru.workinprogress.telek.persistence

import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import ru.workinprogress.telek.FinalState
import ru.workinprogress.telek.State
import ru.workinprogress.telek.StateStorage
import ru.workinprogress.telek.UpdateResult
import ru.workinprogress.telek.UserStateStore

/**
 * See [UserStateStore]'s contract note: [Telek][ru.workinprogress.telek.Telek] never calls
 * [update] concurrently for the same `chatId`, so this store does no per-chat locking of its own.
 * The [lock] below guards nothing but the in-memory cache's own consistency across *different*
 * chatIds, which genuinely are concurrent — the equivalent of the `ConcurrentHashMap` this used to
 * be before the module went multiplatform.
 */
class PersistableUserStateStoreImpl<T : State>(
    val stateStorage: StateStorage<T>,
) : UserStateStore {
    private val lock = SynchronizedObject()
    private val states = mutableMapOf<Long, State>()

    private fun cached(chatId: Long): State? = synchronized(lock) { states[chatId] }

    private fun cache(
        chatId: Long,
        state: State,
    ) = synchronized(lock) { states[chatId] = state }

    private fun evict(chatId: Long) = synchronized(lock) { states.remove(chatId) }

    override suspend fun get(chatId: Long): State? =
        cached(chatId) ?: run {
            val loaded = stateStorage.load(chatId)
            if (loaded != null) {
                cache(chatId, loaded)
            }
            loaded
        }

    override suspend fun update(
        chatId: Long,
        block: suspend (State?) -> UpdateResult,
    ): UpdateResult {
        val current =
            cached(chatId) ?: run {
                val loaded = stateStorage.load(chatId)
                if (loaded != null) cache(chatId, loaded)
                loaded
            }

        val updateResult = block(current)

        if (updateResult.newState is FinalState) {
            stateStorage.delete(chatId)
            evict(chatId)
        } else {
            cache(chatId, updateResult.newState)
            @Suppress("UNCHECKED_CAST")
            stateStorage.save(chatId, updateResult.newState as T)
        }

        return updateResult
    }

    override suspend fun clear(chatId: Long) {
        stateStorage.delete(chatId)
        evict(chatId)
    }
}
