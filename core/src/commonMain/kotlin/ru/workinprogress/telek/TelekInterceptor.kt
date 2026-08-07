package ru.workinprogress.telek

interface TelekInterceptor {
    fun onBeforeInput(
        chatId: Long,
        input: Input,
    ) {
    }

    fun onAfterStateChanged(
        chatId: Long,
        oldState: State?,
        newState: State,
    ) {
    }

    fun onError(
        chatId: Long,
        input: Input?,
        error: Throwable,
    ) {
    }
}
