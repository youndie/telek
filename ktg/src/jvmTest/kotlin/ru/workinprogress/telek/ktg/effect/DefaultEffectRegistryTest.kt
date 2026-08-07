package ru.workinprogress.telek.ktg.effect

import ru.workinprogress.telek.EffectExecutorImpl
import ru.workinprogress.telek.ktg.KtgContextSource
import ru.workinprogress.telek.ktg.effect.handler.EditMarkupEffectHandler
import ru.workinprogress.telek.ktg.effect.handler.EditMessageEffectHandler
import ru.workinprogress.telek.ktg.effect.handler.SendMessageEffectHandler
import kotlin.test.Test
import kotlin.test.assertIs

class DefaultEffectRegistryTest {
    @Test
    fun `defaultEffectRegistry registers a handler for every built-in ktg effect`() {
        val registry = defaultEffectRegistry()

        assertIs<SendMessageEffectHandler>(registry.get(SendMessageEffect::class))
        assertIs<EditMessageEffectHandler>(registry.get(EditMessageEffect::class))
        assertIs<EditMarkupEffectHandler>(registry.get(EditMarkupEffect::class))
    }

    @Test
    fun `ktgEffectExecutor builds a working EffectExecutorImpl`() {
        val executor = ktgEffectExecutor(KtgContextSource())

        assertIs<EffectExecutorImpl>(executor)
    }
}
