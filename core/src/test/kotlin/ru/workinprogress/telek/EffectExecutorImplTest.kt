package ru.workinprogress.telek

import kotlinx.coroutines.test.runTest
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
    fun `effect with registered handler returns handler result`() =
        runTest {
            val registry = EffectRegistry()
            val handler = RecordingEffectHandler { EffectSuccess }
            registry.register(TestEffect::class, handler)
            val executor = EffectExecutorImpl(registry, context = { TestExecutionContext })

            val results = executor.execute(listOf(TestEffect("a")))

            assertEquals(listOf(EffectSuccess), results)
            assertEquals(listOf(TestEffect("a")), handler.handled)
        }

    @Test
    fun `effect without handler is reported as EffectFailed`() =
        runTest {
            val executor = EffectExecutorImpl(EffectRegistry(), context = { TestExecutionContext })

            val results = executor.execute(listOf(TestEffect("missing")))

            val failed = assertIs<EffectFailed>(results.single())
            assertIs<IllegalStateException>(failed.error)
            assertTrue(
                failed.error.message
                    .orEmpty()
                    .contains("EffectHandler not found"),
            )
        }

    @Test
    fun `handler exception is captured as EffectFailed without losing the item`() =
        runTest {
            val registry = EffectRegistry()
            val boom = IllegalArgumentException("boom")
            registry.register(
                TestEffect::class,
                RecordingEffectHandler { throw boom },
            )
            val executor = EffectExecutorImpl(registry, context = { TestExecutionContext })

            val results = executor.execute(listOf(TestEffect("a")))

            val failed = assertIs<EffectFailed>(results.single())
            assertSame(boom, failed.error)
        }

    @Test
    fun `results preserve input order across success, failure and missing handler`() =
        runTest {
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
            val executor = EffectExecutorImpl(registry, context = { TestExecutionContext })

            val results =
                executor.execute(
                    listOf(TestEffect("ok"), TestEffect("throws"), NoHandlerEffect),
                )

            assertEquals(3, results.size)
            assertEquals(EffectSuccess, results[0])
            assertIs<EffectFailed>(results[1]).let { assertSame(boom, it.error) }
            assertIs<EffectFailed>(results[2])
        }

    @Test
    fun `FAIL_FAST policy stops after the first failure`() =
        runTest {
            val registry = EffectRegistry()
            val boom = IllegalStateException("boom")
            registry.register(
                TestEffect::class,
                RecordingEffectHandler { effect ->
                    if (effect.tag == "throws") throw boom else EffectSuccess
                },
            )
            val executor =
                EffectExecutorImpl(
                    registry,
                    context = { TestExecutionContext },
                    failurePolicy = EffectFailurePolicy.FAIL_FAST,
                )

            val results =
                executor.execute(
                    listOf(TestEffect("ok"), TestEffect("throws"), TestEffect("never runs")),
                )

            assertEquals(2, results.size)
            assertEquals(EffectSuccess, results[0])
            assertIs<EffectFailed>(results[1])
        }

    @Test
    fun `empty effects list produces empty results list`() =
        runTest {
            val executor = EffectExecutorImpl(EffectRegistry(), context = { TestExecutionContext })

            val results = executor.execute(emptyList())

            assertTrue(results.isEmpty())
        }

    private object NoHandlerEffect : Effect
}
