package ru.workinprogress.telek.telegram.effect.handler

import com.github.kotlintelegrambot.Bot
import io.mockk.mockk
import ru.workinprogress.telek.EffectResult
import ru.workinprogress.telek.EffectSuccess
import ru.workinprogress.telek.ExecutionContext
import ru.workinprogress.telek.telegram.TelegramContext
import ru.workinprogress.telek.telegram.effect.TelegramEffect
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

private data class FakeTelegramEffect(
    val tag: String,
) : TelegramEffect

private class FakeHandler : TelegramEffectHandler<FakeTelegramEffect> {
    var handledWith: Bot? = null

    override fun handle(
        bot: Bot,
        effect: FakeTelegramEffect,
    ): EffectResult {
        handledWith = bot
        return EffectSuccess
    }
}

private object NotTelegramContext : ExecutionContext

class TelegramEffectHandlerContractTest {
    @Test
    fun `handle throws when the context is not a TelegramContext`() {
        val handler = FakeHandler()

        assertFailsWith<IllegalArgumentException> {
            handler.handle(NotTelegramContext, FakeTelegramEffect("x"))
        }
    }

    @Test
    fun `handle delegates to the bot-based overload when the context is a TelegramContext`() {
        val handler = FakeHandler()
        val bot = mockk<Bot>()
        val context = TelegramContext(bot)

        val result = handler.handle(context, FakeTelegramEffect("x"))

        assertSame(bot, handler.handledWith)
        assertSame(EffectSuccess, result)
    }
}
