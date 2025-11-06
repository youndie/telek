package ru.workinprogress.telek

sealed interface Input {
    data class Message(
        val chatId: Long,
        val text: String,
    ) : Input

    data class Callback(
        val chatId: Long,
        val messageId: Long,
        val data: String,
    ) : Input
}
