package ru.workinprogress.telek.telegram.effect

import ru.workinprogress.telek.telegram.TelegramContextSource
import ru.workinprogress.telek.telegram.effect.handler.EditMarkupEffectHandler
import ru.workinprogress.telek.telegram.effect.handler.EditMessageEffectHandler
import ru.workinprogress.telek.telegram.effect.handler.SendMessageEffectHandler
import kotlin.test.Test
import kotlin.test.assertIs

class DefaultEffectRegistryTest {
    @Test
    fun `defaultEffectRegistry registers a handler for every built-in telegram effect`() {
        val registry = defaultEffectRegistry()

        assertIs<SendMessageEffectHandler>(registry.get(SendMessageEffect::class))
        assertIs<EditMessageEffectHandler>(registry.get(EditMessageEffect::class))
        assertIs<EditMarkupEffectHandler>(registry.get(EditMarkupEffect::class))
    }

    @Test
    fun `telegramEffectExecutor builds a working EffectExecutorImpl`() {
        val executor = telegramEffectExecutor(TelegramContextSource())

        assertIs<ru.workinprogress.telek.EffectExecutorImpl>(executor)
    }
}
