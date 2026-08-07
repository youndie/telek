package ru.workinprogress.telek

import ru.workinprogress.telek.support.OtherState
import ru.workinprogress.telek.support.TestEvent
import ru.workinprogress.telek.support.TestState
import kotlin.reflect.KClass
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

private class RecordingDispatcher(
    private val entryFn: (Input) -> TransitionResult<TestState>? = { null },
) : StateDispatcher<TestState>() {
    override val startCommand = "test"
    override val stateClass: KClass<TestState> = TestState::class

    var transitionCalls = 0
    val onEffectResultCalls = mutableListOf<EffectResult>()

    override fun entry(input: Input): TransitionResult<TestState>? = entryFn(input)

    override fun transition(
        state: TestState,
        input: Input,
    ): TransitionResult<TestState> {
        transitionCalls++
        return noTransition(state)
    }

    override fun onEffectResult(
        state: State,
        effectResult: EffectResult,
    ) {
        onEffectResultCalls += effectResult
    }
}

class StateDispatcherTest {
    private val input = Message(chatId = 1, text = "hi")

    @Test
    fun `handle returns entry result without invoking transition`() {
        val entryResult = TransitionResult<TestState>(TestState.Confirming(1))
        val dispatcher = RecordingDispatcher(entryFn = { entryResult })

        val result = dispatcher.handle(TestState.Waiting(0), input)

        assertEquals(entryResult, result)
        assertEquals(0, dispatcher.transitionCalls)
    }

    @Test
    fun `handle invokes transition when entry is null and state matches`() {
        val dispatcher = RecordingDispatcher()

        val result = dispatcher.handle(TestState.Waiting(0), input)

        assertEquals(1, dispatcher.transitionCalls)
        assertEquals(TestState.Waiting(0), result?.newState)
    }

    @Test
    fun `handle returns null for a state of another type`() {
        val dispatcher = RecordingDispatcher()

        val result = dispatcher.handle(OtherState(0), input)

        assertNull(result)
        assertEquals(0, dispatcher.transitionCalls)
    }

    @Test
    fun `canHandleCallback defaults to comparing with startCommand`() {
        val dispatcher = RecordingDispatcher()

        assertTrue(dispatcher.canHandleCallback("test"))
        assertFalse(dispatcher.canHandleCallback("other"))
    }

    @Test
    fun `onEffectResults invokes onEffectResult with the last result`() {
        val dispatcher = RecordingDispatcher()

        dispatcher.onEffectResults(TestState.Waiting(0), listOf(EffectSuccess, EffectFailed(IllegalStateException())))

        assertEquals(1, dispatcher.onEffectResultCalls.size)
        assertTrue(dispatcher.onEffectResultCalls.single() is EffectFailed)
    }

    @Test
    fun `onEffectResults does nothing for an empty list`() {
        val dispatcher = RecordingDispatcher()

        dispatcher.onEffectResults(TestState.Waiting(0), emptyList())

        assertTrue(dispatcher.onEffectResultCalls.isEmpty())
    }

    @Test
    fun `handleEvent defaults to no transition when the dispatcher doesn't override it`() {
        val dispatcher = RecordingDispatcher()
        val state = TestState.Waiting(3)

        val result = dispatcher.handleEvent(state, TestEvent(chatId = 1, tag = "whatever"))

        assertEquals(state, result?.newState)
    }

    @Test
    fun `handleEvent returns null for a state of another type`() {
        val dispatcher = RecordingDispatcher()

        val result = dispatcher.handleEvent(OtherState(0), TestEvent(chatId = 1, tag = "whatever"))

        assertNull(result)
    }
}
