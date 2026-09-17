package io.github.youndie.telek

/**
 * What a conversation's state is filed under.
 *
 * **Not the same thing as the Telegram address a reply is sent to**, even though in a private chat
 * the two carry the same number. [Input.chatId], [Event.chatId] and the `chatId` on every effect
 * are the *address* — where a message goes. This is the *key* — whose flow the state belongs to.
 * In a private chat there is only one person, so keying by the chat is keying by the person and
 * the difference never shows. In a group it shows immediately: keyed by the chat alone, two
 * members running the same wizard share one state, one worker and one inbox, and each one's reply
 * advances the other's flow.
 *
 * [userId] is nullable rather than a second type so that "this flow belongs to the whole chat"
 * stays expressible — a poll, a group game, anything whose state is genuinely shared. [chat] and
 * [chatAndUser] say which is meant at the call site; the raw constructor is there for a transport
 * that already holds both numbers.
 */
public data class ConversationKey(
    public val chatId: Long,
    public val userId: Long? = null,
) {
    /**
     * A stable, file-name-safe rendering, for a [StateStorage] that has to name something after a
     * key. **Stable** is the load-bearing word: it ends up in file names that a later process reads
     * back, so changing it is a data migration and not a refactor.
     */
    public val storageId: String
        get() = if (userId == null) chatId.toString() else "$chatId.$userId"

    public companion object {
        /** One state for the whole chat — every member of a group shares it. */
        public fun chat(chatId: Long): ConversationKey = ConversationKey(chatId, userId = null)

        /** One state per person per chat. This is what a wizard wants. */
        public fun chatAndUser(
            chatId: Long,
            userId: Long,
        ): ConversationKey = ConversationKey(chatId, userId)
    }
}

/**
 * How a transport turns an incoming update into a [ConversationKey].
 *
 * The default is [PerUserInChat] and that is a deliberate choice rather than a neutral one: it is
 * correct in a group and indistinguishable from [PerChat] in a private chat, where the only sender
 * is the only member. A bot that genuinely wants shared state in a group asks for [PerChat]; a bot
 * that wants something else again — per forum topic, say — builds the key itself and calls
 * [Telek.onInput] directly, which is what the input adapters are public for.
 */
public enum class Keying {
    /**
     * `chatId` plus the sender. An update with no identifiable sender — a channel post, an
     * automatic forward — has no user to key by and falls back to the chat.
     */
    PerUserInChat,

    /** `chatId` alone, whoever sent it. */
    PerChat,
    ;

    public fun key(
        chatId: Long,
        userId: Long?,
    ): ConversationKey =
        when (this) {
            PerUserInChat -> {
                if (userId ==
                    null
                ) {
                    ConversationKey.chat(chatId)
                } else {
                    ConversationKey.chatAndUser(chatId, userId)
                }
            }

            PerChat -> {
                ConversationKey.chat(chatId)
            }
        }
}
