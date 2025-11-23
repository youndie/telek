package ru.workinprogress.telek.persistence

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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

class PersistableUserStateStoreImpl<T : State>(
    val stateStorage: FileStateStorage<T>,
) : UserStateStore {
    private val states = ConcurrentHashMap<Long, State>()
    private val mutexes = ConcurrentHashMap<Long, Mutex>()

    override suspend fun get(chatId: Long): State? =
        mutexes.computeIfAbsent(chatId) { Mutex() }.withLock {
            states[chatId] ?: run {
                val loaded = stateStorage.load(chatId)
                if (loaded != null) {
                    states[chatId] = loaded
                }
                loaded
            }
        }

    override suspend fun update(
        chatId: Long,
        block: suspend (State?) -> UpdateResult,
    ): UpdateResult =
        mutexes.computeIfAbsent(chatId) { Mutex() }.withLock {
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
                stateStorage.save(chatId, updateResult.newState as T)
            }

            updateResult
        }

    override suspend fun clear(chatId: Long) {
        mutexes.computeIfAbsent(chatId) { Mutex() }.withLock {
            stateStorage.delete(chatId)
            states.remove(chatId)
        }
    }
}
