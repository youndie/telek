package ru.workinprogress.telek

/**
 * Something that happened asynchronously and needs to re-enter the FSM for a chat — the result of
 * an [AsyncEffectHandler], not something a user sent. Unlike [Input], an [Event] can never start a
 * flow: it's routed purely by the chat's current state (see [StateDispatcher.transition] overload
 * that takes an [Event]), never by command or callback data.
 */
interface Event {
    val chatId: Long
}
