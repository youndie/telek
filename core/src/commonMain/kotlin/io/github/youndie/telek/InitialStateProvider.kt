package io.github.youndie.telek

public fun interface InitialStateProvider {
    public fun initialState(key: ConversationKey): State
}
