package ru.workinprogress.telek.ktg.effect.handler

import dev.inmo.tgbotapi.bot.TelegramBot
import dev.inmo.tgbotapi.requests.edit.reply_markup.EditChatMessageReplyMarkup
import dev.inmo.tgbotapi.types.ChatId
import dev.inmo.tgbotapi.types.MessageId
import dev.inmo.tgbotapi.types.RawChatId
import dev.inmo.tgbotapi.types.message.abstracts.ContentMessage
import dev.inmo.tgbotapi.types.message.content.MessageContent
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.runBlocking
import ru.workinprogress.telek.ktg.effect.EditMarkupEffect
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertSame

class EditMarkupEffectHandlerTest {
    private val handler = EditMarkupEffectHandler()

    @Test
    fun `a null markup clears the keyboard and returns an EditMarkupEffectResult`() =
        runBlocking {
            val bot = mockk<TelegramBot>()
            val request = slot<EditChatMessageReplyMarkup>()
            coEvery { bot.execute(capture(request)) } returns mockk<ContentMessage<MessageContent>>()

            val result = handler.handle(bot, EditMarkupEffect(chatId = 1, messageId = 2, markup = null))

            assertEquals(ChatId(RawChatId(1)), request.captured.chatId)
            assertEquals(MessageId(2), request.captured.messageId)
            assertNull(request.captured.replyMarkup)

            val success = assertIs<EditMarkupEffectResult>(result)
            assertEquals(1L, success.chatId)
            assertEquals(2L, success.messageId)
        }

    @Test
    fun `an API failure propagates so the executor reports it as EffectFailed`() =
        runBlocking {
            val bot = mockk<TelegramBot>()
            val boom = RuntimeException("boom")
            coEvery { bot.execute(any<EditChatMessageReplyMarkup>()) } throws boom

            val thrown =
                assertFailsWith<RuntimeException> {
                    handler.handle(bot, EditMarkupEffect(chatId = 1, messageId = 2, markup = null))
                }

            assertSame(boom, thrown)
        }
}
