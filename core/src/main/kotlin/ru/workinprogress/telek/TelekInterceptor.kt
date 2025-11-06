package ru.workinprogress.telek

interface TelekInterceptor {
    suspend fun onBeforeInput(
        chatId: Long,
        input: Input,
    ) {
    }

    suspend fun onAfterStateChanged(
        chatId: Long,
        oldState: State?,
        newState: State,
    ) {
    }

    suspend fun onError(
        chatId: Long,
        input: Input?,
        error: Throwable,
    ) {
    }
}
