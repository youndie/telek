package ru.workinprogress.telek.telegram.effect.handler

import com.github.kotlintelegrambot.Bot
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import ru.workinprogress.telek.Event
import ru.workinprogress.telek.ExecutionContext
import ru.workinprogress.telek.telegram.TelegramContext
import ru.workinprogress.telek.telegram.effect.TelegramEffect
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertSame

private data class FakeAsyncTelegramEffect(
    val tag: String,
) : TelegramEffect

private data class FakeAsyncEvent(
    override val chatId: Long,
) : Event

private class FakeAsyncHandler : TelegramAsyncEffectHandler<FakeAsyncTelegramEffect> {
    var handledWith: Bot? = null

    override suspend fun handle(
        bot: Bot,
        effect: FakeAsyncTelegramEffect,
    ): Event {
        handledWith = bot
        return FakeAsyncEvent(chatId = 1)
    }
}

private object NotTelegramContextForAsync : ExecutionContext

class TelegramAsyncEffectHandlerContractTest {
    @Test
    fun `handle throws when the context is not a TelegramContext`(): Unit =
        runBlocking {
            val handler = FakeAsyncHandler()

            assertFailsWith<IllegalArgumentException> {
                handler.handle(NotTelegramContextForAsync, FakeAsyncTelegramEffect("x"))
            }
        }

    @Test
    fun `handle delegates to the bot-based overload when the context is a TelegramContext`() =
        runBlocking {
            val handler = FakeAsyncHandler()
            val bot = mockk<Bot>()
            val context = TelegramContext(bot)

            val result = handler.handle(context, FakeAsyncTelegramEffect("x"))

            assertSame(bot, handler.handledWith)
            assertEquals(1L, assertIs<FakeAsyncEvent>(result).chatId)
        }
}
