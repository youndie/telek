package ru.workinprogress.telek.ktg.effect.handler

import dev.inmo.tgbotapi.bot.TelegramBot
import dev.inmo.tgbotapi.requests.edit.text.EditChatMessageText
import dev.inmo.tgbotapi.types.ChatId
import dev.inmo.tgbotapi.types.MessageId
import dev.inmo.tgbotapi.types.RawChatId
import dev.inmo.tgbotapi.types.message.MarkdownParseMode
import dev.inmo.tgbotapi.types.message.abstracts.ContentMessage
import dev.inmo.tgbotapi.types.message.content.TextContent
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.runBlocking
import ru.workinprogress.telek.ktg.effect.EditMessageEffect
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertSame

class EditMessageEffectHandlerTest {
    private val handler = EditMessageEffectHandler()

    @Test
    fun `success returns an EditMessageEffectResult for the edited message`() =
        runBlocking {
            val bot = mockk<TelegramBot>()
            val request = slot<EditChatMessageText>()
            coEvery { bot.execute(capture(request)) } returns mockk<ContentMessage<TextContent>>()

            val result = handler.handle(bot, EditMessageEffect(chatId = 1, messageId = 2, text = "edited"))

            assertEquals(ChatId(RawChatId(1)), request.captured.chatId)
            assertEquals(MessageId(2), request.captured.messageId)
            assertEquals("edited", request.captured.text)
            assertEquals(MarkdownParseMode, request.captured.parseMode)

            val success = assertIs<EditMessageEffectResult>(result)
            assertEquals(1L, success.chatId)
            assertEquals(2L, success.messageId)
        }

    @Test
    fun `an API failure propagates so the executor reports it as EffectFailed`() =
        runBlocking {
            val bot = mockk<TelegramBot>()
            val boom = RuntimeException("boom")
            coEvery { bot.execute(any<EditChatMessageText>()) } throws boom

            val thrown =
                assertFailsWith<RuntimeException> {
                    handler.handle(bot, EditMessageEffect(chatId = 1, messageId = 2, text = "edited"))
                }

            assertSame(boom, thrown)
        }
}
