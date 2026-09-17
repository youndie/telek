package io.github.youndie.telek.telegram

import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.dispatcher.Dispatcher
import com.github.kotlintelegrambot.dispatcher.handlers.Handler
import com.github.kotlintelegrambot.entities.CallbackQuery
import com.github.kotlintelegrambot.entities.Chat
import com.github.kotlintelegrambot.entities.Message
import com.github.kotlintelegrambot.entities.Update
import com.github.kotlintelegrambot.entities.User
import com.github.kotlintelegrambot.types.TelegramBotResult
import io.github.youndie.telek.Callback
import io.github.youndie.telek.ConversationKey
import io.github.youndie.telek.Keying
import io.github.youndie.telek.Telek
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertSame
import io.github.youndie.telek.Message as TelekMessage

/**
 * [Dispatcher] has an internal constructor, so it can't be built directly here.
 * MockK creates an instance without calling any constructor (via Objenesis), which is enough
 * since [connect] only calls the public [Dispatcher.addHandler] on it.
 *
 * The concrete `MessageHandler`/`CallbackQueryHandler` types [connect] registers are themselves
 * `internal` to kotlin-telegram-bot, so tests can't `filterIsInstance` for them — instead, each
 * captured [Handler] is identified by [Handler.checkUpdate] against the [Update] it's meant to
 * handle, which is exactly what [Dispatcher] itself uses to route an incoming update.
 */
class ConnectTest {
    private fun chat(id: Long) = Chat(id = id, type = "private")

    private fun message(
        chatId: Long,
        text: String?,
        messageId: Long = 100,
        from: User? = null,
    ) = Message(messageId = messageId, date = 0, chat = chat(chatId), text = text, from = from)

    private fun user(id: Long = 1) = User(id = id, isBot = false, firstName = "Test")

    private fun capturedHandlers(
        telek: Telek,
        contextSource: TelegramContextSource,
        keying: Keying = Keying.PerUserInChat,
    ): List<Handler> {
        val handlers = mutableListOf<Handler>()
        val dispatcher = mockk<Dispatcher>(relaxed = true)
        every { dispatcher.addHandler(any()) } answers { handlers += firstArg<Handler>() }

        dispatcher.connect(telek, contextSource, keying)

        return handlers
    }

    @Test
    fun `message handler provides the bot to the context source and forwards chatId and text to onInput`() =
        runBlocking {
            val bot = mockk<Bot>(relaxed = true)
            val telek = mockk<Telek>(relaxed = true)
            val contextSource = TelegramContextSource()
            val update = Update(updateId = 1, message = message(chatId = 42, text = "hello"))
            val messageHandler = capturedHandlers(telek, contextSource).single { it.checkUpdate(update) }

            messageHandler.handleUpdate(bot, update)

            assertSame(bot, contextSource.context().bot)
            verify { telek.onInput(key = ConversationKey.chat(42), input = TelekMessage(chatId = 42, text = "hello")) }
        }

    @Test
    fun `message handler defaults to an empty string when the message has no text`() =
        runBlocking {
            val bot = mockk<Bot>(relaxed = true)
            val telek = mockk<Telek>(relaxed = true)
            val update = Update(updateId = 1, message = message(chatId = 42, text = null))
            val messageHandler = capturedHandlers(telek, TelegramContextSource()).single { it.checkUpdate(update) }

            messageHandler.handleUpdate(bot, update)

            verify { telek.onInput(key = ConversationKey.chat(42), input = TelekMessage(chatId = 42, text = "")) }
        }

    @Test
    fun `callbackQuery handler forwards chatId, messageId and data to onInput`() =
        runBlocking {
            val bot = mockk<Bot>(relaxed = true)
            every {
                bot.answerCallbackQuery(any(), any(), any(), any(), any())
            } returns TelegramBotResult.Success(true)
            val telek = mockk<Telek>(relaxed = true)

            val callbackQuery =
                CallbackQuery(
                    id = "cb1",
                    from = user(),
                    message = message(chatId = 7, text = null, messageId = 55),
                    data = "route:data",
                    chatInstance = "inst",
                )
            val update = Update(updateId = 2, callbackQuery = callbackQuery)
            val callbackHandler = capturedHandlers(telek, TelegramContextSource()).single { it.checkUpdate(update) }
            callbackHandler.handleUpdate(bot, update)

            verify {
                telek.onInput(
                    key = ConversationKey.chatAndUser(chatId = 7, userId = 1),
                    input = Callback(chatId = 7, messageId = 55, data = "route:data"),
                )
            }
        }

    @Test
    fun `two members of one chat are two keys, and both carry the same address`() =
        runBlocking {
            val bot = mockk<Bot>(relaxed = true)
            val telek = mockk<Telek>(relaxed = true)

            listOf(11L, 22L).forEach { userId ->
                val update =
                    Update(updateId = 1, message = message(chatId = -100, text = "hi", from = user(userId)))
                capturedHandlers(telek, TelegramContextSource())
                    .single { it.checkUpdate(update) }
                    .handleUpdate(bot, update)
            }

            // Two keys, so two states and two per-conversation actors — and one address, because
            // both replies go to the same group.
            listOf(11L, 22L).forEach { userId ->
                verify {
                    telek.onInput(
                        key = ConversationKey.chatAndUser(chatId = -100, userId = userId),
                        input = TelekMessage(chatId = -100, text = "hi"),
                    )
                }
            }
        }

    @Test
    fun `Keying PerChat files everyone in a chat under the chat itself`() =
        runBlocking {
            val bot = mockk<Bot>(relaxed = true)
            val telek = mockk<Telek>(relaxed = true)
            val update =
                Update(updateId = 1, message = message(chatId = -100, text = "hi", from = user(11)))

            capturedHandlers(telek, TelegramContextSource(), Keying.PerChat)
                .single { it.checkUpdate(update) }
                .handleUpdate(bot, update)

            verify {
                telek.onInput(
                    key = ConversationKey.chat(-100),
                    input = TelekMessage(chatId = -100, text = "hi"),
                )
            }
        }

    @Test
    fun `callbackQuery handler does nothing when the callback has no attached message`() =
        runBlocking {
            val bot = mockk<Bot>(relaxed = true)
            every {
                bot.answerCallbackQuery(any(), any(), any(), any(), any())
            } returns TelegramBotResult.Success(true)
            val telek = mockk<Telek>(relaxed = true)

            val callbackQuery =
                CallbackQuery(id = "cb1", from = user(), message = null, data = "route:data", chatInstance = "inst")
            val update = Update(updateId = 3, callbackQuery = callbackQuery)
            val callbackHandler = capturedHandlers(telek, TelegramContextSource()).single { it.checkUpdate(update) }
            callbackHandler.handleUpdate(bot, update)

            verify(exactly = 0) { telek.onInput(any(), any()) }
        }
}
