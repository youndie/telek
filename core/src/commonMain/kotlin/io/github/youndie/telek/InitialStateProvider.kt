package io.github.youndie.telek

fun interface InitialStateProvider {
    fun initialState(chatId: Long): State
}
