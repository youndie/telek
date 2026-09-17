package io.github.youndie.telek

import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized

/**
 * Stores one FSM state per [ConversationKey] — which is a chat, or a person within a chat; see
 * [ConversationKey] for why that is not the same as the chat a reply is addressed to.
 *
 * [update] is only ever called by [Telek], through its internal per-conversation actor
 * ([ChatWorkers]), which guarantees that at most one [update] call for a given key is in flight at
 * any time, in the order the corresponding inputs were received. Implementations do not need to —
 * and should not — serialize [update] internally with their own per-key locking: that duplicates a
 * guarantee the caller already provides, and doing it locally is easy to get subtly wrong (e.g.
 * reading state before acquiring a lock, or leaking a lock/mutex map that's never cleaned up).
 */
public interface UserStateStore {
    public suspend fun get(key: ConversationKey): State?

    public suspend fun update(
        key: ConversationKey,
        block: suspend (State?) -> UpdateResult,
    ): UpdateResult

    public suspend fun clear(key: ConversationKey)
}

// `internal`, not `public`: this is the default `Telek` falls back to, and a consumer either
// takes it by not passing one or supplies their own `UserStateStore` — which stays public. The
// only code that names this class is this module and its tests.
internal class DefaultUserStateStore : UserStateStore {
    // Per the contract above this never needs to serialize *one conversation's* update against
    // itself — only to keep the map itself consistent across different keys, which are genuinely
    // concurrent. A short non-suspending critical section around each map operation is all that
    // takes; `block` is deliberately invoked outside it.
    private val lock = SynchronizedObject()
    private val states = mutableMapOf<ConversationKey, State>()

    override suspend fun get(key: ConversationKey): State? = synchronized(lock) { states[key] }

    override suspend fun update(
        key: ConversationKey,
        block: suspend (State?) -> UpdateResult,
    ): UpdateResult {
        val current = synchronized(lock) { states[key] }
        val updateResult = block(current)

        synchronized(lock) {
            if (updateResult.newState is FinalState) {
                states.remove(key)
            } else {
                states[key] = updateResult.newState
            }
        }

        return updateResult
    }

    override suspend fun clear(key: ConversationKey) {
        synchronized(lock) { states.remove(key) }
    }
}
