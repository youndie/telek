package ru.workinprogress.telek

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap

interface UserStateStore {
    suspend fun get(chatId: Long): State?

    suspend fun update(
        chatId: Long,
        block: suspend (State?) -> State,
    )

    suspend fun clear(chatId: Long)
}

class DefaultUserStateStore : UserStateStore {
    private val states = ConcurrentHashMap<Long, State>()
    private val mutexes = ConcurrentHashMap<Long, Mutex>()

    override suspend fun get(chatId: Long): State? = states[chatId]

    override suspend fun update(
        chatId: Long,
        block: suspend (State?) -> State,
    ) {
        val current = get(chatId)
        val mutex = mutexes.computeIfAbsent(chatId) { Mutex() }

        mutex.withLock {
            val newState = block(current)

            if (newState is FinalState) {
                clear(chatId)
                return
            }

            states[chatId] = newState
        }
    }

    override suspend fun clear(chatId: Long) {
        states.remove(chatId)
        mutexes.remove(chatId)
    }
}
