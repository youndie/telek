package io.github.youndie.telek

/**
 * Something that happened asynchronously and needs to re-enter the FSM — the result of
 * an [AsyncEffectHandler], not something a user sent. Unlike [Input], an [Event] can never start a
 * flow: it's routed purely by the conversation's current state (see [StateDispatcher.transition]
 * overload that takes an [Event]), never by command or callback data.
 *
 * [chatId] is an address, like [Input.chatId] — it says where a handler's next message goes, not
 * which conversation the event belongs to. That is the [ConversationKey] the async work was
 * launched for, and telek routes by it without asking the event.
 */
public interface Event {
    public val chatId: Long
}
