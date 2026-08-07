package ru.workinprogress.telek.ktg

import dev.inmo.tgbotapi.bot.TelegramBot
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertSame

class KtgContextSourceTest {
    @Test
    fun `a bot passed to the constructor is available immediately`() =
        runBlocking {
            val bot = mockk<TelegramBot>()

            assertSame(bot, KtgContextSource(bot).context().bot)
        }

    @Test
    fun `provide resolves a source built before the bot existed`() =
        runBlocking {
            val bot = mockk<TelegramBot>()
            val source = KtgContextSource()

            source.provide(bot)

            assertSame(bot, source.context().bot)
        }

    @Test
    fun `the first provided bot wins`() =
        runBlocking {
            val first = mockk<TelegramBot>()
            val source = KtgContextSource(first)

            source.provide(mockk<TelegramBot>())

            assertSame(first, source.context().bot)
        }
}
