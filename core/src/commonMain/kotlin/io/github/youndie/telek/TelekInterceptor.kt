package io.github.youndie.telek

public interface TelekInterceptor {
    public fun onBeforeInput(
        key: ConversationKey,
        input: Input,
    ) {
    }

    public fun onAfterStateChanged(
        key: ConversationKey,
        oldState: State?,
        newState: State,
    ) {
    }

    public fun onError(
        key: ConversationKey,
        input: Input?,
        error: Throwable,
    ) {
    }
}
