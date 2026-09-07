package io.github.youndie.telek.ktg.effect.handler

import dev.inmo.tgbotapi.bot.TelegramBot
import io.github.youndie.telek.EffectResult
import io.github.youndie.telek.EffectSuccess
import io.github.youndie.telek.ExecutionContext
import io.github.youndie.telek.ktg.KtgContext
import io.github.youndie.telek.ktg.effect.KtgEffect
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

private data class FakeKtgEffect(
    val tag: String,
) : KtgEffect

private class FakeHandler : KtgEffectHandler<FakeKtgEffect> {
    var handledWith: TelegramBot? = null

    override suspend fun handle(
        bot: TelegramBot,
        effect: FakeKtgEffect,
    ): EffectResult {
        handledWith = bot
        return EffectSuccess
    }
}

private object NotKtgContext : ExecutionContext

class KtgEffectHandlerContractTest {
    @Test
    fun `handle throws when the context is not a KtgContext`(): Unit =
        runBlocking {
            val handler = FakeHandler()

            assertFailsWith<IllegalArgumentException> {
                handler.handle(NotKtgContext, FakeKtgEffect("x"))
            }
        }

    @Test
    fun `handle delegates to the bot-based overload when the context is a KtgContext`() =
        runBlocking {
            val handler = FakeHandler()
            val bot = mockk<TelegramBot>()

            val result = handler.handle(KtgContext(bot), FakeKtgEffect("x"))

            assertSame(bot, handler.handledWith)
            assertSame(EffectSuccess, result)
        }
}
