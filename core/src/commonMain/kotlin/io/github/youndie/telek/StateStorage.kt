package io.github.youndie.telek

public interface StateStorage<S : State> {
    public suspend fun save(
        chatId: Long,
        state: S,
    )

    public suspend fun load(chatId: Long): S?

    public suspend fun delete(chatId: Long)
}
