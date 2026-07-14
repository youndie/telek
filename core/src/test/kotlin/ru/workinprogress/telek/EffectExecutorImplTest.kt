package ru.workinprogress.telek

import ru.workinprogress.telek.support.RecordingEffectHandler
import ru.workinprogress.telek.support.TestEffect
import ru.workinprogress.telek.support.TestExecutionContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlin.test.assertTrue

class EffectExecutorImplTest {
    @Test
    fun `effect with registered handler returns handler result`() {
        val registry = EffectRegistry()
        val handler = RecordingEffectHandler { EffectSuccess }
        registry.register(TestEffect::class, handler)
        val executor = EffectExecutorImpl(registry)

        val results = executor.execute(TestExecutionContext, listOf(TestEffect("a")))

        assertEquals(listOf(EffectSuccess), results)
        assertEquals(listOf(TestEffect("a")), handler.handled)
    }

    @Test
    fun `effect without handler is reported as EffectFailed`() {
        val executor = EffectExecutorImpl(EffectRegistry())

        val results = executor.execute(TestExecutionContext, listOf(TestEffect("missing")))

        val failed = assertIs<EffectFailed>(results.single())
        assertIs<IllegalStateException>(failed.error)
        assertTrue(
            failed.error.message
                .orEmpty()
                .contains("EffectHandler not found"),
        )
    }

    @Test
    fun `handler exception is captured as EffectFailed without losing the item`() {
        val registry = EffectRegistry()
        val boom = IllegalArgumentException("boom")
        registry.register(
            TestEffect::class,
            RecordingEffectHandler { throw boom },
        )
        val executor = EffectExecutorImpl(registry)

        val results = executor.execute(TestExecutionContext, listOf(TestEffect("a")))

        val failed = assertIs<EffectFailed>(results.single())
        assertSame(boom, failed.error)
    }

    @Test
    fun `results preserve input order across success, failure and missing handler`() {
        val registry = EffectRegistry()
        val boom = IllegalStateException("boom")
        registry.register(
            TestEffect::class,
            RecordingEffectHandler { effect ->
                when (effect.tag) {
                    "ok" -> EffectSuccess
                    "throws" -> throw boom
                    else -> EffectSuccess
                }
            },
        )
        val executor = EffectExecutorImpl(registry)

        val results =
            executor.execute(
                TestExecutionContext,
                listOf(TestEffect("ok"), TestEffect("throws"), NoHandlerEffect),
            )

        assertEquals(3, results.size)
        assertEquals(EffectSuccess, results[0])
        assertIs<EffectFailed>(results[1]).let { assertSame(boom, it.error) }
        assertIs<EffectFailed>(results[2])
    }

    @Test
    fun `empty effects list produces empty results list`() {
        val executor = EffectExecutorImpl(EffectRegistry())

        val results = executor.execute(TestExecutionContext, emptyList())

        assertTrue(results.isEmpty())
    }

    private object NoHandlerEffect : Effect
}
