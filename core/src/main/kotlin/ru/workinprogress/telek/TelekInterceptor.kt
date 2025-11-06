package ru.workinprogress.telek

interface TelekInterceptor {
    suspend fun onBeforeEvent(
        chatId: Long,
        input: Input,
    ) {}

    suspend fun onAfterEvent(
        chatId: Long,
        oldState: State?,
        newState: State,
    ) {}

    suspend fun onError(
        chatId: Long,
        input: Input,
        error: Throwable,
    ) {}
}
