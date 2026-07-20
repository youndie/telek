package ru.workinprogress.telek.telegram.effect.handler

import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.entities.ChatId
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import ru.workinprogress.telek.EffectFailed
import ru.workinprogress.telek.telegram.effect.EditMarkupEffect
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertSame

class EditMarkupEffectHandlerTest {
    private val handler = EditMarkupEffectHandler()

    @Test
    fun `success returns EditMarkupEffectResult`() =
        runBlocking {
            val bot = mockk<Bot>()
            every {
                bot.editMessageReplyMarkup(
                    chatId = ChatId.fromId(1),
                    messageId = 2,
                    replyMarkup = null,
                )
            } returns (null to null)

            val result = handler.handle(bot, EditMarkupEffect(chatId = 1, messageId = 2, markup = null))

            val markupResult = assertIs<EditMarkupEffectResult>(result)
            assertEquals(1, markupResult.chatId)
            assertEquals(2, markupResult.messageId)
            verify {
                bot.editMessageReplyMarkup(
                    chatId = ChatId.fromId(1),
                    messageId = 2,
                    replyMarkup = null,
                )
            }
        }

    @Test
    fun `failure returns EffectFailed with the underlying exception`() =
        runBlocking {
            val bot = mockk<Bot>()
            val exception = RuntimeException("boom")
            every {
                bot.editMessageReplyMarkup(
                    chatId = ChatId.fromId(1),
                    messageId = 2,
                    replyMarkup = null,
                )
            } returns (null to exception)

            val result = handler.handle(bot, EditMarkupEffect(chatId = 1, messageId = 2, markup = null))

            val failed = assertIs<EffectFailed>(result)
            assertSame(exception, failed.error)
        }
}
