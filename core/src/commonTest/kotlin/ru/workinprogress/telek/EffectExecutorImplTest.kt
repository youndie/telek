package ru.workinprogress.telek

import kotlinx.coroutines.test.runTest
import ru.workinprogress.telek.support.RecordingAsyncEffectHandler
import ru.workinprogress.telek.support.RecordingEffectHandler
import ru.workinprogress.telek.support.TestDebouncedEffect
import ru.workinprogress.telek.support.TestEffect
import ru.workinprogress.telek.support.TestEvent
import ru.workinprogress.telek.support.TestExecutionContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
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

            val results = executor.execute(listOf(TestEffect("a"))) { _, _ -> }

            assertEquals(listOf(EffectSuccess), results)
            assertEquals(listOf(TestEffect("a")), handler.handled)
        }

    @Test
    fun `effect without handler is reported as EffectFailed`() =
        runTest {
            val executor = EffectExecutorImpl(EffectRegistry(), context = { TestExecutionContext })

            val results = executor.execute(listOf(TestEffect("missing"))) { _, _ -> }

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

            val results = executor.execute(listOf(TestEffect("a"))) { _, _ -> }

            val failed = assertIs<EffectFailed>(results.single())
            assertSame(boom, failed.error)
        }

    @Test
    fun `results preserve input order across success - failure - missing handler`() =
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
                ) { _, _ -> }

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
                ) { _, _ -> }

            assertEquals(2, results.size)
            assertEquals(EffectSuccess, results[0])
            assertIs<EffectFailed>(results[1])
        }

    @Test
    fun `empty effects list produces empty results list`() =
        runTest {
            val executor = EffectExecutorImpl(EffectRegistry(), context = { TestExecutionContext })

            val results = executor.execute(emptyList()) { _, _ -> }

            assertTrue(results.isEmpty())
        }

    @Test
    fun `an effect registered as async is dispatched via dispatchAsync rather than run inline`() =
        runTest {
            val registry = EffectRegistry()
            val handler = RecordingAsyncEffectHandler()
            registry.registerAsync(TestEffect::class, handler)
            val executor = EffectExecutorImpl(registry, context = { TestExecutionContext })
            val dispatched = mutableListOf<suspend () -> Event?>()

            val results = executor.execute(listOf(TestEffect("a"))) { _, work -> dispatched += work }

            assertTrue(results.isEmpty())
            assertTrue(handler.handled.isEmpty()) // not run inline — only once `dispatched` is invoked
            assertEquals(1, dispatched.size)
        }

    @Test
    fun `invoking the dispatched work runs the async handler and returns its event`() =
        runTest {
            val registry = EffectRegistry()
            val expectedEvent = TestEvent(chatId = 1, tag = "loaded")
            registry.registerAsync(TestEffect::class, RecordingAsyncEffectHandler { expectedEvent })
            val executor = EffectExecutorImpl(registry, context = { TestExecutionContext })
            var work: (suspend () -> Event?)? = null

            executor.execute(listOf(TestEffect("a"))) { _, w -> work = w }

            assertSame(expectedEvent, work?.invoke())
        }

    @Test
    fun `a throwing async handler is caught and logged and its work yields null instead of propagating`() =
        runTest {
            val registry = EffectRegistry()
            val boom = RuntimeException("boom")
            registry.registerAsync(TestEffect::class, RecordingAsyncEffectHandler { throw boom })
            var loggedError: Throwable? = null
            val logger =
                object : TelekLogger {
                    override fun log(
                        level: TelekLogLevel,
                        message: String,
                        error: Throwable?,
                    ) {
                        if (level == TelekLogLevel.ERROR) loggedError = error
                    }
                }
            val executor = EffectExecutorImpl(registry, context = { TestExecutionContext }, logger = logger)
            var work: (suspend () -> Event?)? = null

            executor.execute(listOf(TestEffect("a"))) { _, w -> work = w }

            assertNull(work?.invoke())
            assertSame(boom, loggedError)
        }

    @Test
    fun `sync and async effects in the same batch don't interfere with each other's results`() =
        runTest {
            val registry = EffectRegistry()
            registry.register(TestEffect::class, RecordingEffectHandler { EffectSuccess })
            // Same effect class can't be both — use a distinct type for the async one.
            registry.registerAsync(
                NoHandlerEffect::class,
                object : AsyncEffectHandler<NoHandlerEffect> {
                    override suspend fun handle(
                        context: ExecutionContext,
                        effect: NoHandlerEffect,
                    ): Event? = null
                },
            )
            val executor = EffectExecutorImpl(registry, context = { TestExecutionContext })

            val results = executor.execute(listOf(TestEffect("a"), NoHandlerEffect)) { _, _ -> }

            assertEquals(listOf(EffectSuccess), results)
        }

    @Test
    fun `an effect implementing Debounced passes its debounceKey to dispatchAsync`() =
        runTest {
            val registry = EffectRegistry()
            registry.registerAsync(
                TestDebouncedEffect::class,
                object : AsyncEffectHandler<TestDebouncedEffect> {
                    override suspend fun handle(
                        context: ExecutionContext,
                        effect: TestDebouncedEffect,
                    ): Event? = null
                },
            )
            val executor = EffectExecutorImpl(registry, context = { TestExecutionContext })
            var capturedKey: Any? = "not set"

            executor.execute(listOf(TestDebouncedEffect("a", debounceKey = "search"))) { key, _ ->
                capturedKey = key
            }

            assertEquals("search", capturedKey)
        }

    @Test
    fun `an async effect that does not implement Debounced passes a null key`() =
        runTest {
            val registry = EffectRegistry()
            registry.registerAsync(TestEffect::class, RecordingAsyncEffectHandler())
            val executor = EffectExecutorImpl(registry, context = { TestExecutionContext })
            var capturedKey: Any? = "not set"

            executor.execute(listOf(TestEffect("a"))) { key, _ -> capturedKey = key }

            assertNull(capturedKey)
        }

    private object NoHandlerEffect : Effect
}
