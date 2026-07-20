package ru.workinprogress.telek

import java.util.concurrent.ConcurrentHashMap

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
    private val states = ConcurrentHashMap<Long, State>()

    override suspend fun get(chatId: Long): State? = states[chatId]

    override suspend fun update(
        chatId: Long,
        block: suspend (State?) -> UpdateResult,
    ): UpdateResult {
        val current = states[chatId]
        val updateResult = block(current)

        if (updateResult.newState is FinalState) {
            states.remove(chatId)
        } else {
            states[chatId] = updateResult.newState
        }

        return updateResult
    }

    override suspend fun clear(chatId: Long) {
        states.remove(chatId)
    }
}
