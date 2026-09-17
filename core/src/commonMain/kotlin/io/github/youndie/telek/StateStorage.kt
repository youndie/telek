package io.github.youndie.telek

public interface StateStorage<S : State> {
    public suspend fun save(
        key: ConversationKey,
        state: S,
    )

    public suspend fun load(key: ConversationKey): S?

    public suspend fun delete(key: ConversationKey)
}
