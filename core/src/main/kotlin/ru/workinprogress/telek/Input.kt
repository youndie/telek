package ru.workinprogress.telek

interface Input {
    val chatId: Long
}

data class Message(
    override val chatId: Long,
    val text: String,
) : Input

data class Callback(
    override val chatId: Long,
    val messageId: Long,
    val data: String,
) : Input
