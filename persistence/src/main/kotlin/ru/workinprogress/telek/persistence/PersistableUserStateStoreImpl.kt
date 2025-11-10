package ru.workinprogress.telek.persistence

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import ru.workinprogress.telek.FinalState
import ru.workinprogress.telek.State
import ru.workinprogress.telek.UserStateStore
import java.util.concurrent.ConcurrentHashMap

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
        block: suspend (State?) -> State,
    ) {
        val current = get(chatId)
        mutexes.computeIfAbsent(chatId) { Mutex() }.withLock {
            val newState = block(current)

            if (newState is FinalState) {
                clear(chatId)
                return
            }

            states[chatId] = newState
            @Suppress("UNCHECKED_CAST")
            stateStorage.save(chatId, newState as T)
        }
    }

    override suspend fun clear(chatId: Long) {
        mutexes.computeIfAbsent(chatId) { Mutex() }.withLock {
            stateStorage.delete(chatId)
            states.remove(chatId)
            mutexes.remove(chatId)
        }
    }
}
