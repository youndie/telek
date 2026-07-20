@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package ru.workinprogress.telek

import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import ru.workinprogress.telek.support.FakeEffectExecutor
import ru.workinprogress.telek.support.RecordingInterceptor
import ru.workinprogress.telek.support.TestEffect
import ru.workinprogress.telek.support.TestEvent
import ru.workinprogress.telek.support.TestState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

private class WizardDispatcher : StateDispatcher<TestState>() {
    override val startCommand = "test"
    override val stateClass = TestState::class

    override fun entry(input: Input): TransitionResult<TestState>? =
        if (input is Message && input.text == "/test") {
            transition {
                newState = TestState.Waiting(0)
                add(TestEffect("started"))
            }
        } else {
            null
        }

    override fun transition(
        state: TestState,
        input: Input,
    ): TransitionResult<TestState> =
        when {
            state is TestState.Waiting && input is Message && input.text == "boom" ->
                throw RuntimeException("boom")

            state is TestState.Waiting && input is Message ->
                transition {
                    newState = TestState.Confirming(value = input.text.length)
                    add(TestEffect("confirm-prompt"))
                }

            state is TestState.Confirming && input is Callback && input.data == "confirm" ->
                transition {
                    newState = TestState.Done(value = state.value)
                    add(TestEffect("done"))
                }

            else -> noTransition(state)
        }

    fun postDirect(
        chatId: Long,
        reducer: (TestState) -> TransitionResult<TestState>,
    ) = transitionGate.post(chatId, reducer)
}

private class AsyncDispatcher : StateDispatcher<TestState>() {
    override val startCommand = "async"
    override val stateClass = TestState::class

    override fun entry(input: Input): TransitionResult<TestState>? =
        if (input is Message && input.text == "/async") {
            transition {
                newState = TestState.Waiting(0)
                add(TestEffect("fetch"))
            }
        } else {
            null
        }

    override fun transition(
        state: TestState,
        input: Input,
    ): TransitionResult<TestState> = noTransition(state)

    override fun transition(
        state: TestState,
        event: Event,
    ): TransitionResult<TestState> =
        if (state is TestState.Waiting && event is TestEvent && event.tag == "loaded") {
            transition { newState = TestState.Confirming(value = 99) }
        } else {
            noTransition(state)
        }
}

class TelekTest {
    @Test
    fun `onInput runs the dispatcher, transitions state and executes effects`() =
        runTest {
            val executor = FakeEffectExecutor()
            val telek = Telek(scope = this, dispatchers = listOf(WizardDispatcher()), effectExecutor = executor)
            telek.onInput(chatId = 1, input = Message(1, "/test"))
            advanceUntilIdle()

            assertEquals(listOf(TestEffect("started")), executor.executed.single())
        }

    @Test
    fun `full README-style scenario transitions Waiting to Confirming to Done`() =
        runTest {
            val executor = FakeEffectExecutor()
            val telek = Telek(scope = this, dispatchers = listOf(WizardDispatcher()), effectExecutor = executor)
            telek.onInput(1, Message(1, "/test"))
            advanceUntilIdle()
            telek.onInput(1, Message(1, "hello"))
            advanceUntilIdle()
            telek.onInput(1, Callback(1, messageId = 1, data = "confirm"))
            advanceUntilIdle()

            val allEffects = executor.executed
            assertEquals(
                listOf<List<Effect>>(
                    listOf(TestEffect("started")),
                    listOf(TestEffect("confirm-prompt")),
                    listOf(TestEffect("done")),
                ),
                allEffects,
            )
        }

    @Test
    fun `reaching a FinalState clears the stored state`() =
        runTest {
            val executor = FakeEffectExecutor()
            val store = DefaultUserStateStore()
            val telek =
                Telek(
                    scope = this,
                    userStateStore = store,
                    dispatchers = listOf(WizardDispatcher()),
                    effectExecutor = executor,
                )
            telek.onInput(1, Message(1, "/test"))
            advanceUntilIdle()
            telek.onInput(1, Message(1, "hello"))
            advanceUntilIdle()
            telek.onInput(1, Callback(1, messageId = 1, data = "confirm"))
            advanceUntilIdle()

            assertNull(store.get(1))
        }

    @Test
    fun `interceptors observe before-input and after-state-changed in order`() =
        runTest {
            val executor = FakeEffectExecutor()
            val interceptor = RecordingInterceptor()
            val telek =
                Telek(
                    scope = this,
                    dispatchers = listOf(WizardDispatcher()),
                    effectExecutor = executor,
                    interceptors = listOf(interceptor),
                )
            val input = Message(1, "/test")
            telek.onInput(1, input)
            advanceUntilIdle()

            assertEquals(1, interceptor.beforeInput.size)
            assertEquals(input, interceptor.beforeInput.single().input)

            assertEquals(1, interceptor.afterStateChanged.size)
            val afterCall = interceptor.afterStateChanged.single()
            assertNull(afterCall.oldState)
            assertEquals(TestState.Waiting(0), afterCall.newState)
        }

    @Test
    fun `an exception thrown during transition is reported to onError and state is left unchanged`() =
        runTest {
            val executor = FakeEffectExecutor()
            val interceptor = RecordingInterceptor()
            val store = DefaultUserStateStore()
            val telek =
                Telek(
                    scope = this,
                    userStateStore = store,
                    dispatchers = listOf(WizardDispatcher()),
                    effectExecutor = executor,
                    interceptors = listOf(interceptor),
                )
            telek.onInput(1, Message(1, "/test"))
            advanceUntilIdle()

            telek.onInput(1, Message(1, "boom"))
            advanceUntilIdle()

            assertEquals(1, interceptor.errors.size)
            val error = interceptor.errors.single()
            assertEquals(1, error.chatId)
            assertIs<RuntimeException>(error.error)
            assertEquals("boom", error.error.message)
            assertEquals(TestState.Waiting(0), store.get(1))
        }

    @Test
    fun `applyReducer on a mismatched state type reports IllegalStateException to onError`() =
        runTest {
            val executor = FakeEffectExecutor()
            val interceptor = RecordingInterceptor()
            val dispatcher = WizardDispatcher()
            val telek =
                Telek(
                    scope = this,
                    dispatchers = listOf(dispatcher),
                    effectExecutor = executor,
                    interceptors = listOf(interceptor),
                )
            // current state is EmptyState, but the gate expects TestState -> mismatch
            dispatcher.postDirect(1) { state -> noTransition(state) }
            advanceUntilIdle()

            assertEquals(1, interceptor.errors.size)
            assertIs<IllegalStateException>(interceptor.errors.single().error)
        }

    @Test
    fun `applyReducer via TransitionGate mutates matching state directly`() =
        runTest {
            val executor = FakeEffectExecutor()
            val store = DefaultUserStateStore()
            val dispatcher = WizardDispatcher()
            val telek =
                Telek(
                    scope = this,
                    userStateStore = store,
                    dispatchers = listOf(dispatcher),
                    effectExecutor = executor,
                )
            telek.onInput(1, Message(1, "/test"))
            advanceUntilIdle()

            dispatcher.postDirect(1) { state ->
                transition { newState = TestState.Confirming(value = 42) }
            }
            advanceUntilIdle()

            assertEquals(TestState.Confirming(42), store.get(1))
        }

    @Test
    fun `dispatcher onEffectResults receives the effect execution results`() =
        runTest {
            val results = mutableListOf<Pair<State, List<EffectResult>>>()
            val dispatcher =
                object : StateDispatcher<TestState>() {
                    override val startCommand = "test"
                    override val stateClass = TestState::class

                    override fun entry(input: Input): TransitionResult<TestState>? =
                        if (input is Message && input.text == "/test") {
                            transition {
                                newState = TestState.Waiting(0)
                                add(TestEffect("started"))
                            }
                        } else {
                            null
                        }

                    override fun transition(
                        state: TestState,
                        input: Input,
                    ): TransitionResult<TestState> = noTransition(state)

                    override fun onEffectResults(
                        state: State,
                        effectResults: List<EffectResult>,
                    ) {
                        results += state to effectResults
                    }
                }
            val executor = FakeEffectExecutor { EffectSuccess }
            val telek = Telek(scope = this, dispatchers = listOf(dispatcher), effectExecutor = executor)
            telek.onInput(1, Message(1, "/test"))
            advanceUntilIdle()

            assertEquals(1, results.size)
            assertEquals(TestState.Waiting(0), results.single().first)
            assertTrue(results.single().second.all { it === EffectSuccess })
        }

    @Test
    fun `a failed effect is reported to onError even though the transition itself succeeded`() =
        runTest {
            val interceptor = RecordingInterceptor()
            val boom = RuntimeException("send failed")
            val executor = FakeEffectExecutor { EffectFailed(boom) }
            val telek =
                Telek(
                    scope = this,
                    dispatchers = listOf(WizardDispatcher()),
                    effectExecutor = executor,
                    interceptors = listOf(interceptor),
                )

            val input = Message(1, "/test")
            telek.onInput(1, input)
            advanceUntilIdle()

            assertEquals(1, interceptor.errors.size)
            val error = interceptor.errors.single()
            assertEquals(1, error.chatId)
            assertEquals(input, error.input)
            assertSame(boom, error.error)
            // The transition itself is unaffected — a failed effect doesn't undo the state change.
            assertEquals(1, interceptor.afterStateChanged.size)
        }

    @Test
    fun `an async effect's event re-enters the FSM through the transition(state, event) overload`() =
        runTest {
            val store = DefaultUserStateStore()
            val executor =
                FakeEffectExecutor(
                    asyncWorkFor = { effect ->
                        if (effect is TestEffect && effect.tag == "fetch") {
                            { TestEvent(chatId = 1, tag = "loaded") }
                        } else {
                            null
                        }
                    },
                )
            val telek =
                Telek(
                    scope = this,
                    userStateStore = store,
                    dispatchers = listOf(AsyncDispatcher()),
                    effectExecutor = executor,
                )

            telek.onInput(1, Message(1, "/async"))
            advanceUntilIdle()

            assertEquals(TestState.Confirming(99), store.get(1))
        }

    @Test
    fun `an event that doesn't match the dispatcher's expectations leaves the state unchanged`() =
        runTest {
            val store = DefaultUserStateStore()
            val executor =
                FakeEffectExecutor(
                    asyncWorkFor = { effect ->
                        if (effect is TestEffect && effect.tag == "fetch") {
                            { TestEvent(chatId = 1, tag = "unexpected") }
                        } else {
                            null
                        }
                    },
                )
            val telek =
                Telek(
                    scope = this,
                    userStateStore = store,
                    dispatchers = listOf(AsyncDispatcher()),
                    effectExecutor = executor,
                )

            telek.onInput(1, Message(1, "/async"))
            advanceUntilIdle()

            assertEquals(TestState.Waiting(0), store.get(1))
        }

    @Test
    fun `an async handler returning null produces no follow-up transition`() =
        runTest {
            val interceptor = RecordingInterceptor()
            val store = DefaultUserStateStore()
            val executor = FakeEffectExecutor(asyncWorkFor = { { null } })
            val telek =
                Telek(
                    scope = this,
                    userStateStore = store,
                    dispatchers = listOf(AsyncDispatcher()),
                    effectExecutor = executor,
                    interceptors = listOf(interceptor),
                )

            telek.onInput(1, Message(1, "/async"))
            advanceUntilIdle()

            assertEquals(TestState.Waiting(0), store.get(1))
            // Only the entry transition — no second, event-triggered one.
            assertEquals(1, interceptor.afterStateChanged.size)
        }
}
