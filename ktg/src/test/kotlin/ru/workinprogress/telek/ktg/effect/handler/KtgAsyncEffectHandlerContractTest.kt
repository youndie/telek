package ru.workinprogress.telek.ktg.effect.handler

import dev.inmo.tgbotapi.bot.TelegramBot
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import ru.workinprogress.telek.Event
import ru.workinprogress.telek.ExecutionContext
import ru.workinprogress.telek.ktg.KtgContext
import ru.workinprogress.telek.ktg.effect.KtgEffect
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertSame

private data class FakeAsyncKtgEffect(
    val tag: String,
) : KtgEffect

private data class FakeAsyncEvent(
    override val chatId: Long,
) : Event

private class FakeAsyncHandler : KtgAsyncEffectHandler<FakeAsyncKtgEffect> {
    var handledWith: TelegramBot? = null

    override suspend fun handle(
        bot: TelegramBot,
        effect: FakeAsyncKtgEffect,
    ): Event {
        handledWith = bot
        return FakeAsyncEvent(chatId = 1)
    }
}

private object NotKtgContextForAsync : ExecutionContext

class KtgAsyncEffectHandlerContractTest {
    @Test
    fun `handle throws when the context is not a KtgContext`(): Unit =
        runBlocking {
            val handler = FakeAsyncHandler()

            assertFailsWith<IllegalArgumentException> {
                handler.handle(NotKtgContextForAsync, FakeAsyncKtgEffect("x"))
            }
        }

    @Test
    fun `handle delegates to the bot-based overload when the context is a KtgContext`() =
        runBlocking {
            val handler = FakeAsyncHandler()
            val bot = mockk<TelegramBot>()

            val result = handler.handle(KtgContext(bot), FakeAsyncKtgEffect("x"))

            assertSame(bot, handler.handledWith)
            assertEquals(1L, assertIs<FakeAsyncEvent>(result).chatId)
        }
}
