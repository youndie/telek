package ru.workinprogress.telek

interface StateStorage<S : State> {
    suspend fun save(
        chatId: Long,
        state: S,
    )

    suspend fun load(chatId: Long): S?

    suspend fun delete(chatId: Long)
}
