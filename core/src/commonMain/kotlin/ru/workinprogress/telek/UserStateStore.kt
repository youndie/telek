package ru.workinprogress.telek

import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized

/**
 * Stores per-chat FSM state.
 *
 * [update] is only ever called by [Telek], through its internal per-chat actor ([ChatWorkers]),
 * which guarantees that at most one [update] call for a given `chatId` is in flight at any time,
 * in the order the corresponding inputs were received. Implementations do not need to — and
 * should not — serialize [update] internally with their own per-chat locking: that duplicates a
 * guarantee the caller already provides, and doing it locally is easy to get subtly wrong (e.g.
 * reading state before acquiring a lock, or leaking a lock/mutex map that's never cleaned up).
 */
interface UserStateStore {
    suspend fun get(chatId: Long): State?

    suspend fun update(
        chatId: Long,
        block: suspend (State?) -> UpdateResult,
    ): UpdateResult

    suspend fun clear(chatId: Long)
}

class DefaultUserStateStore : UserStateStore {
    // Per the contract above this never needs to serialize *one chat's* update against itself —
    // only to keep the map itself consistent across different chatIds, which are genuinely
    // concurrent. A short non-suspending critical section around each map operation is all that
    // takes; `block` is deliberately invoked outside it.
    private val lock = SynchronizedObject()
    private val states = mutableMapOf<Long, State>()

    override suspend fun get(chatId: Long): State? = synchronized(lock) { states[chatId] }

    override suspend fun update(
        chatId: Long,
        block: suspend (State?) -> UpdateResult,
    ): UpdateResult {
        val current = synchronized(lock) { states[chatId] }
        val updateResult = block(current)

        synchronized(lock) {
            if (updateResult.newState is FinalState) {
                states.remove(chatId)
            } else {
                states[chatId] = updateResult.newState
            }
        }

        return updateResult
    }

    override suspend fun clear(chatId: Long) {
        synchronized(lock) { states.remove(chatId) }
    }
}
