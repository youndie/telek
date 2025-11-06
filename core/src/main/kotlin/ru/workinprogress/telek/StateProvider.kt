package ru.workinprogress.telek

fun interface StateProvider {
    fun initialState(chatId: Long): State
}
