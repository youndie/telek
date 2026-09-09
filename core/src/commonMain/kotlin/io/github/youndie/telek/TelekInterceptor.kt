package io.github.youndie.telek

public interface TelekInterceptor {
    public fun onBeforeInput(
        chatId: Long,
        input: Input,
    ) {
    }

    public fun onAfterStateChanged(
        chatId: Long,
        oldState: State?,
        newState: State,
    ) {
    }

    public fun onError(
        chatId: Long,
        input: Input?,
        error: Throwable,
    ) {
    }
}
