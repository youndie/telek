package io.github.youndie.telek

public fun interface InitialStateProvider {
    public fun initialState(chatId: Long): State
}
