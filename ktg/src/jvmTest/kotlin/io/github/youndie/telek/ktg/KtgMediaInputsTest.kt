package io.github.youndie.telek.ktg

import dev.inmo.tgbotapi.requests.abstracts.FileId
import dev.inmo.tgbotapi.types.ChatId
import dev.inmo.tgbotapi.types.MessageId
import dev.inmo.tgbotapi.types.RawChatId
import dev.inmo.tgbotapi.types.TgFileUniqueId
import dev.inmo.tgbotapi.types.chat.PrivateChatImpl
import dev.inmo.tgbotapi.types.files.DocumentFile
import dev.inmo.tgbotapi.types.files.FileSize
import dev.inmo.tgbotapi.types.files.PhotoSize
import dev.inmo.tgbotapi.types.location.StaticLocation
import dev.inmo.tgbotapi.types.message.abstracts.ContentMessage
import dev.inmo.tgbotapi.types.message.content.ContactContent
import dev.inmo.tgbotapi.types.message.content.DocumentContent
import dev.inmo.tgbotapi.types.message.content.LocationContent
import dev.inmo.tgbotapi.types.message.content.MessageContent
import dev.inmo.tgbotapi.types.message.content.PhotoContent
import dev.inmo.tgbotapi.utils.MimeType
import dev.inmo.tgbotapi.utils.RiskFeature
import io.mockk.every
import io.mockk.mockk
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import dev.inmo.tgbotapi.types.Contact as KtgContact

/**
 * The adapters for the input types B-03 added. Content objects are built for real rather than
 * mocked wherever ktgbotapi's constructors are public — a mock of a value-class accessor would
 * mostly be testing mockk.
 */
@OptIn(RiskFeature::class)
class KtgMediaInputsTest {
    private fun <T : MessageContent> message(
        content: T,
        chatId: Long = 42,
        messageId: Long = 100,
    ) = mockk<ContentMessage<T>> {
        every { chat } returns PrivateChatImpl(ChatId(RawChatId(chatId)))
        every { this@mockk.content } returns content
        every { this@mockk.messageId } returns MessageId(messageId)
    }

    private fun photoSize(
        id: String,
        width: Int,
        height: Int,
    ) = PhotoSize(
        fileId = FileId(id),
        fileUniqueId = TgFileUniqueId("unique-$id"),
        fileSize = FileSize(1024u),
        width = width,
        height = height,
    )

    // PhotoContent's collection is a value class over the size list, which cannot be built from a
    // test without reaching into ktgbotapi's internals -- so the content itself is mocked, at
    // exactly the two members the adapter reads.
    @Test
    fun `a photo carries the file the transport designates plus its caption`() {
        val content =
            mockk<PhotoContent> {
                every { media } returns photoSize("scan", 1280, 1280)
                every { text } returns "look"
            }

        val input = message(content).asTelekInput()

        assertEquals(42, input.chatId)
        assertEquals(100, input.messageId)
        assertEquals("scan", input.file.fileId)
        assertEquals("unique-scan", input.file.uniqueId)
        assertEquals(1024L, input.file.sizeBytes)
        assertEquals("look", input.caption)
    }

    @Test
    fun `a photo with no caption carries none`() {
        val content =
            mockk<PhotoContent> {
                every { media } returns photoSize("scan", 90, 90)
                every { text } returns null
            }

        assertNull(message(content).asTelekInput().caption)
    }

    @Test
    fun `a document carries its name and mime type alongside the file`() {
        val documentFile =
            DocumentFile(
                fileId = FileId("doc-1"),
                fileUniqueId = TgFileUniqueId("unique-doc-1"),
                fileSize = FileSize(2048u),
                fileName = "passport.pdf",
                mimeType = MimeType("application/pdf"),
            )
        val content = DocumentContent(media = documentFile, text = "here")

        val input = message(content).asTelekInput()

        assertEquals("doc-1", input.file.fileId)
        assertEquals(2048L, input.file.sizeBytes)
        assertEquals("passport.pdf", input.fileName)
        assertEquals("application/pdf", input.mimeType)
        assertEquals("here", input.caption)
    }

    @Test
    fun `a contact carries the number and the name`() {
        val content =
            ContactContent(
                KtgContact(
                    phoneNumber = "+70000000000",
                    firstName = "Ada",
                    lastName = "Lovelace",
                    userId = ChatId(RawChatId(7)),
                ),
            )

        val input = message(content).asTelekInput()

        assertEquals("+70000000000", input.phoneNumber)
        assertEquals("Ada", input.firstName)
        assertEquals("Lovelace", input.lastName)
        assertEquals(7L, input.userId)
    }

    @Test
    fun `a contact from the address book has no telegram user behind it`() {
        val content = ContactContent(KtgContact(phoneNumber = "+70000000001", firstName = "Someone"))

        assertNull(message(content).asTelekInput().userId)
    }

    @Test
    fun `a location carries latitude and longitude the right way round`() {
        val content = LocationContent(StaticLocation(latitude = 55.75, longitude = 37.62))

        val input = message(content).asTelekInput()

        assertEquals(55.75, input.latitude)
        assertEquals(37.62, input.longitude)
    }
}
