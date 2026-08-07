package ru.workinprogress.telek

import ru.workinprogress.telek.support.RecordingEffectHandler
import ru.workinprogress.telek.support.TestEffect
import kotlin.test.Test
import kotlin.test.assertNull
import kotlin.test.assertSame

class EffectRegistryTest {
    @Test
    fun `register and get returns the same handler`() {
        val registry = EffectRegistry()
        val handler = RecordingEffectHandler()

        registry.register(TestEffect::class, handler)

        assertSame(handler, registry.get(TestEffect::class))
    }

    @Test
    fun `get returns null for unregistered effect class`() {
        val registry = EffectRegistry()

        assertNull(registry.get(TestEffect::class))
    }

    @Test
    fun `re-registering overwrites the previous handler`() {
        val registry = EffectRegistry()
        val first = RecordingEffectHandler()
        val second = RecordingEffectHandler()

        registry.register(TestEffect::class, first)
        registry.register(TestEffect::class, second)

        assertSame(second, registry.get(TestEffect::class))
    }
}
