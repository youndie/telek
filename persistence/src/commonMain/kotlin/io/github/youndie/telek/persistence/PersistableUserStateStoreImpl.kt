package io.github.youndie.telek.persistence

import io.github.youndie.telek.ConversationKey
import io.github.youndie.telek.FinalState
import io.github.youndie.telek.State
import io.github.youndie.telek.StateStorage
import io.github.youndie.telek.UpdateResult
import io.github.youndie.telek.UserStateStore
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized

/**
 * See [UserStateStore]'s contract note: [Telek][io.github.youndie.telek.Telek] never calls
 * [update] concurrently for the same [ConversationKey], so this store does no per-key locking of
 * its own. The [lock] below guards nothing but the in-memory cache's own consistency across
 * *different* keys, which genuinely are concurrent — the equivalent of the `ConcurrentHashMap`
 * this used to be before the module went multiplatform.
 */
public class PersistableUserStateStoreImpl<T : State>(
    public val stateStorage: StateStorage<T>,
) : UserStateStore {
    private val lock = SynchronizedObject()
    private val states = mutableMapOf<ConversationKey, State>()

    private fun cached(key: ConversationKey): State? = synchronized(lock) { states[key] }

    private fun cache(
        key: ConversationKey,
        state: State,
    ) = synchronized(lock) { states[key] = state }

    private fun evict(key: ConversationKey) = synchronized(lock) { states.remove(key) }

    override suspend fun get(key: ConversationKey): State? =
        cached(key) ?: run {
            val loaded = stateStorage.load(key)
            if (loaded != null) {
                cache(key, loaded)
            }
            loaded
        }

    override suspend fun update(
        key: ConversationKey,
        block: suspend (State?) -> UpdateResult,
    ): UpdateResult {
        val current =
            cached(key) ?: run {
                val loaded = stateStorage.load(key)
                if (loaded != null) cache(key, loaded)
                loaded
            }

        val updateResult = block(current)

        if (updateResult.newState is FinalState) {
            stateStorage.delete(key)
            evict(key)
        } else {
            cache(key, updateResult.newState)
            @Suppress("UNCHECKED_CAST")
            stateStorage.save(key, updateResult.newState as T)
        }

        return updateResult
    }

    override suspend fun clear(key: ConversationKey) {
        stateStorage.delete(key)
        evict(key)
    }
}
