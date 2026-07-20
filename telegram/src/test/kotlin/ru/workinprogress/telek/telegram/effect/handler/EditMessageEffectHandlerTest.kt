package ru.workinprogress.telek.telegram.effect.handler

import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.ParseMode
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import ru.workinprogress.telek.EffectFailed
import ru.workinprogress.telek.telegram.effect.EditMessageEffect
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertSame

class EditMessageEffectHandlerTest {
    private val handler = EditMessageEffectHandler()

    @Test
    fun `success returns an EditMessageEffectResult`() =
        runBlocking {
            val bot = mockk<Bot>()
            every {
                bot.editMessageText(
                    chatId = ChatId.fromId(1),
                    messageId = 2,
                    text = "edited",
                    parseMode = ParseMode.MARKDOWN,
                    replyMarkup = null,
                )
            } returns (null to null)

            val result = handler.handle(bot, EditMessageEffect(chatId = 1, messageId = 2, text = "edited"))

            val success = assertIs<EditMessageEffectResult>(result)
            assertEquals(1, success.chatId)
            assertEquals(2, success.messageId)
            verify {
                bot.editMessageText(
                    chatId = ChatId.fromId(1),
                    messageId = 2,
                    text = "edited",
                    parseMode = ParseMode.MARKDOWN,
                    replyMarkup = null,
                )
            }
        }

    @Test
    fun `failure is reported as EffectFailed, not an EffectResult subtype`() =
        runBlocking {
            val bot = mockk<Bot>()
            val exception = RuntimeException("boom")
            every {
                bot.editMessageText(
                    chatId = ChatId.fromId(1),
                    messageId = 2,
                    text = "edited",
                    parseMode = ParseMode.MARKDOWN,
                    replyMarkup = null,
                )
            } returns (null to exception)

            val result = handler.handle(bot, EditMessageEffect(chatId = 1, messageId = 2, text = "edited"))

            val failed = assertIs<EffectFailed>(result)
            assertSame(exception, failed.error)
        }
}
