package io.github.youndie.telek

/**
 * Something a user sent.
 *
 * [chatId] is the Telegram **address** — the chat a reply is sent to — and not the key the
 * conversation's state is filed under; in a group those are different things. See
 * [ConversationKey], and [Telek.onInput], which takes the key separately.
 *
 * **Not sealed, and that is the design.** A bot that needs something telek does not model declares
 * its own `Input` and feeds it through [Telek.onInput]; it routes by the conversation's state like
 * any other input that is not a command or a callback (see [DefaultFindDispatcherStrategy]), and
 * nothing in `:core` needs to change. The same property is what lets telek add a type here without
 * breaking a consumer's `when`.
 */
public interface Input {
    public val chatId: Long
}

/**
 * A file Telegram holds, named the way Telegram names one.
 *
 * telek does not download it — that is an effect, and effects are where a transport belongs. What a
 * dispatcher gets is the identifier it needs to ask for the bytes later, without naming a transport
 * type to do it.
 *
 * [fileId] is only valid for the bot that received it and can change between messages; [uniqueId]
 * is stable across both and is what to compare or store when the question is "is this the same
 * file". Telegram documents that distinction and bots get it wrong in that direction, so both are
 * carried rather than only the one a caller reaches for first.
 */
public data class FileRef(
    public val fileId: String,
    public val uniqueId: String? = null,
    public val sizeBytes: Long? = null,
)

public data class Message(
    override val chatId: Long,
    val text: String,
) : Input

public data class Callback(
    override val chatId: Long,
    val messageId: Long,
    val data: String,
) : Input

/**
 * A photo. Telegram offers several sizes of one; [file] is the one the transport designates, and
 * telek models no others — a bot that needs a particular size asks the transport for it, which is
 * out of this model on purpose.
 */
public data class Photo(
    override val chatId: Long,
    val messageId: Long,
    val file: FileRef,
    val caption: String? = null,
) : Input

public data class Document(
    override val chatId: Long,
    val messageId: Long,
    val file: FileRef,
    val fileName: String? = null,
    val mimeType: String? = null,
    val caption: String? = null,
) : Input

/**
 * A contact, which is how "share your phone number" arrives.
 *
 * [userId] is set when the contact is a Telegram user and null when it is an entry from the
 * sender's address book — so it is not a way to learn who sent the message. That is the
 * [ConversationKey] the input was submitted under.
 */
public data class Contact(
    override val chatId: Long,
    val messageId: Long,
    val phoneNumber: String,
    val firstName: String,
    val lastName: String? = null,
    val userId: Long? = null,
) : Input

public data class Location(
    override val chatId: Long,
    val messageId: Long,
    val latitude: Double,
    val longitude: Double,
) : Input
