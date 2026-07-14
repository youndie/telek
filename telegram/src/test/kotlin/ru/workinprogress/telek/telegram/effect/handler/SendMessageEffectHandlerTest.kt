package ru.workinprogress.telek.telegram.effect.handler

import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.Message
import com.github.kotlintelegrambot.entities.ParseMode
import com.github.kotlintelegrambot.types.TelegramBotResult
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import ru.workinprogress.telek.telegram.effect.SendMessageEffect
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class SendMessageEffectHandlerTest {
    private val handler = SendMessageEffectHandler()

    @Test
    fun `successful send returns a SendMessageEffectResult with the new messageId`() {
        val bot = mockk<Bot>()
        val message = mockk<Message> { every { messageId } returns 42L }
        every {
            bot.sendMessage(
                chatId = ChatId.fromId(1),
                text = "hi",
                parseMode = ParseMode.MARKDOWN,
                replyMarkup = null,
            )
        } returns TelegramBotResult.Success(message)

        val result = handler.handle(bot, SendMessageEffect(chatId = 1, text = "hi"))

        val success = assertIs<SendMessageEffectResult>(result)
        assertEquals(1, success.chatId)
        assertEquals(42L, success.messageId)
        verify {
            bot.sendMessage(
                chatId = ChatId.fromId(1),
                text = "hi",
                parseMode = ParseMode.MARKDOWN,
                replyMarkup = null,
            )
        }
    }

    @Test
    fun `failed send returns a TelegramEffectError`() {
        val bot = mockk<Bot>()
        val error = TelegramBotResult.Error.Unknown(RuntimeException("boom"))
        every {
            bot.sendMessage(
                chatId = ChatId.fromId(1),
                text = "hi",
                parseMode = ParseMode.MARKDOWN,
                replyMarkup = null,
            )
        } returns error

        val result = handler.handle(bot, SendMessageEffect(chatId = 1, text = "hi"))

        val failed = assertIs<TelegramEffectError>(result)
        assertEquals(1, failed.chatId)
        assertEquals(error, failed.error)
    }
}
