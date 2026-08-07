package ru.workinprogress.telek.ktg.effect.handler

import dev.inmo.tgbotapi.bot.TelegramBot
import dev.inmo.tgbotapi.requests.send.SendTextMessage
import dev.inmo.tgbotapi.types.ChatId
import dev.inmo.tgbotapi.types.MessageId
import dev.inmo.tgbotapi.types.RawChatId
import dev.inmo.tgbotapi.types.message.MarkdownParseMode
import dev.inmo.tgbotapi.types.message.abstracts.PrivateContentMessage
import dev.inmo.tgbotapi.types.message.content.TextContent
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.runBlocking
import ru.workinprogress.telek.ktg.effect.SendMessageEffect
import ru.workinprogress.telek.ktg.inlineKeyboard
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertSame

class SendMessageEffectHandlerTest {
    private val handler = SendMessageEffectHandler()

    @Test
    fun `successful send returns a SendMessageEffectResult with the new messageId`() =
        runBlocking {
            val bot = mockk<TelegramBot>()
            val sent = mockk<PrivateContentMessage<TextContent>> { every { messageId } returns MessageId(42) }
            val request = slot<SendTextMessage>()
            coEvery { bot.execute(capture(request)) } returns sent

            val result = handler.handle(bot, SendMessageEffect(chatId = 1, text = "hi"))

            assertEquals(ChatId(RawChatId(1)), request.captured.chatId)
            assertEquals("hi", request.captured.text)
            assertEquals(MarkdownParseMode, request.captured.parseMode)
            assertNull(request.captured.replyMarkup)

            val success = assertIs<SendMessageEffectResult>(result)
            assertEquals(1L, success.chatId)
            assertEquals(42L, success.messageId)
        }

    @Test
    fun `the markup is passed through as the request's replyMarkup`() =
        runBlocking {
            val bot = mockk<TelegramBot>()
            val sent = mockk<PrivateContentMessage<TextContent>> { every { messageId } returns MessageId(42) }
            val request = slot<SendTextMessage>()
            coEvery { bot.execute(capture(request)) } returns sent
            val markup = inlineKeyboard { row { callback("Yes", "yes") } }

            handler.handle(bot, SendMessageEffect(chatId = 1, text = "hi", markup = markup))

            assertEquals(markup, request.captured.replyMarkup)
        }

    @Test
    fun `an API failure propagates so the executor reports it as EffectFailed`() =
        runBlocking {
            val bot = mockk<TelegramBot>()
            val boom = RuntimeException("boom")
            coEvery { bot.execute(any<SendTextMessage>()) } throws boom

            val thrown =
                assertFailsWith<RuntimeException> {
                    handler.handle(bot, SendMessageEffect(chatId = 1, text = "hi"))
                }

            assertSame(boom, thrown)
        }
}
