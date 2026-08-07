package ru.workinprogress.telek.ktg

import dev.inmo.tgbotapi.types.ChatId
import dev.inmo.tgbotapi.types.MessageId
import dev.inmo.tgbotapi.types.RawChatId
import dev.inmo.tgbotapi.types.chat.PrivateChatImpl
import dev.inmo.tgbotapi.types.message.abstracts.ContentMessage
import dev.inmo.tgbotapi.types.message.content.TextContent
import dev.inmo.tgbotapi.types.queries.callback.InlineMessageIdDataCallbackQuery
import dev.inmo.tgbotapi.types.queries.callback.MessageDataCallbackQuery
import dev.inmo.tgbotapi.utils.RiskFeature
import io.mockk.every
import io.mockk.mockk
import ru.workinprogress.telek.Callback
import ru.workinprogress.telek.Message
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(RiskFeature::class)
class KtgInputsTest {
    private fun textMessage(
        chatId: Long,
        text: String,
        messageId: Long = 100,
    ) = mockk<ContentMessage<TextContent>> {
        every { chat } returns PrivateChatImpl(ChatId(RawChatId(chatId)))
        every { content } returns TextContent(text)
        every { this@mockk.messageId } returns MessageId(messageId)
    }

    @Test
    fun `telekChatId unwraps ktgbotapi's value classes down to a plain Long`() {
        assertEquals(42L, textMessage(chatId = 42, text = "hello").telekChatId)
    }

    @Test
    fun `a text message becomes a telek Message carrying chatId and text`() {
        val input = textMessage(chatId = 42, text = "hello").asTelekInput()

        assertEquals(Message(chatId = 42, text = "hello"), input)
    }

    @Test
    fun `a data callback query with a message becomes a telek Callback`() {
        val query =
            mockk<MessageDataCallbackQuery> {
                every { message } returns textMessage(chatId = 7, text = "", messageId = 55)
                every { data } returns "route:data"
            }

        assertEquals(Callback(chatId = 7, messageId = 55, data = "route:data"), query.asTelekInput())
    }

    @Test
    fun `a callback query without an attached message cannot be routed and maps to null`() {
        val query = mockk<InlineMessageIdDataCallbackQuery> { every { data } returns "route:data" }

        assertNull(query.asTelekInput())
    }
}
