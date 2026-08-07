package ru.workinprogress.telek

fun interface InitialStateProvider {
    fun initialState(chatId: Long): State
}
