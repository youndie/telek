package ru.workinprogress.telek.telegram

import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.dispatcher.Dispatcher
import com.github.kotlintelegrambot.dispatcher.handlers.CallbackQueryHandler
import com.github.kotlintelegrambot.dispatcher.handlers.Handler
import com.github.kotlintelegrambot.dispatcher.handlers.MessageHandler
import com.github.kotlintelegrambot.entities.CallbackQuery
import com.github.kotlintelegrambot.entities.Chat
import com.github.kotlintelegrambot.entities.Message
import com.github.kotlintelegrambot.entities.Update
import com.github.kotlintelegrambot.entities.User
import com.github.kotlintelegrambot.types.TelegramBotResult
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import ru.workinprogress.telek.Callback
import ru.workinprogress.telek.Telek
import kotlin.test.Test
import ru.workinprogress.telek.Message as TelekMessage

/**
 * [Dispatcher] has an internal constructor, so it can't be built directly here.
 * MockK creates an instance without calling any constructor (via Objenesis), which is enough
 * since [connect] only calls the public [Dispatcher.addHandler] on it.
 */
class ConnectTest {
    private fun chat(id: Long) = Chat(id = id, type = "private")

    private fun message(
        chatId: Long,
        text: String?,
        messageId: Long = 100,
    ) = Message(messageId = messageId, date = 0, chat = chat(chatId), text = text)

    private fun user() = User(id = 1, isBot = false, firstName = "Test")

    private fun capturedHandlers(telek: Telek): List<Handler> {
        val handlers = mutableListOf<Handler>()
        val dispatcher = mockk<Dispatcher>(relaxed = true)
        every { dispatcher.addHandler(any()) } answers { handlers += firstArg<Handler>() }

        dispatcher.connect(telek)

        return handlers
    }

    @Test
    fun `message handler initializes context and forwards chatId and text to onInput`() =
        runBlocking {
            val bot = mockk<Bot>(relaxed = true)
            val telek = mockk<Telek>(relaxed = true)
            val messageHandler = capturedHandlers(telek).filterIsInstance<MessageHandler>().single()

            messageHandler.handleUpdate(bot, Update(updateId = 1, message = message(chatId = 42, text = "hello")))

            verify { telek.initIfNeeded(any()) }
            verify { telek.onInput(chatId = 42, input = TelekMessage(chatId = 42, text = "hello")) }
        }

    @Test
    fun `message handler defaults to an empty string when the message has no text`() =
        runBlocking {
            val bot = mockk<Bot>(relaxed = true)
            val telek = mockk<Telek>(relaxed = true)
            val messageHandler = capturedHandlers(telek).filterIsInstance<MessageHandler>().single()

            messageHandler.handleUpdate(bot, Update(updateId = 1, message = message(chatId = 42, text = null)))

            verify { telek.onInput(chatId = 42, input = TelekMessage(chatId = 42, text = "")) }
        }

    @Test
    fun `callbackQuery handler forwards chatId, messageId and data to onInput`() =
        runBlocking {
            val bot = mockk<Bot>(relaxed = true)
            every {
                bot.answerCallbackQuery(any(), any(), any(), any(), any())
            } returns TelegramBotResult.Success(true)
            val telek = mockk<Telek>(relaxed = true)
            val callbackHandler = capturedHandlers(telek).filterIsInstance<CallbackQueryHandler>().single()

            val callbackQuery =
                CallbackQuery(
                    id = "cb1",
                    from = user(),
                    message = message(chatId = 7, text = null, messageId = 55),
                    data = "route:data",
                    chatInstance = "inst",
                )
            callbackHandler.handleUpdate(bot, Update(updateId = 2, callbackQuery = callbackQuery))

            verify { telek.onInput(chatId = 7, input = Callback(chatId = 7, messageId = 55, data = "route:data")) }
        }

    @Test
    fun `callbackQuery handler does nothing when the callback has no attached message`() =
        runBlocking {
            val bot = mockk<Bot>(relaxed = true)
            every {
                bot.answerCallbackQuery(any(), any(), any(), any(), any())
            } returns TelegramBotResult.Success(true)
            val telek = mockk<Telek>(relaxed = true)
            val callbackHandler = capturedHandlers(telek).filterIsInstance<CallbackQueryHandler>().single()

            val callbackQuery =
                CallbackQuery(id = "cb1", from = user(), message = null, data = "route:data", chatInstance = "inst")
            callbackHandler.handleUpdate(bot, Update(updateId = 3, callbackQuery = callbackQuery))

            verify(exactly = 0) { telek.onInput(any(), any()) }
        }
}
