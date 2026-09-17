@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package io.github.youndie.telek

import io.github.youndie.telek.support.FakeEffectExecutor
import io.github.youndie.telek.support.RecordingInterceptor
import io.github.youndie.telek.support.TestEffect
import io.github.youndie.telek.support.TestEvent
import io.github.youndie.telek.support.TestState
import io.github.youndie.telek.support.key
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
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
            state is TestState.Waiting && input is Message && input.text == "boom" -> {
                throw RuntimeException("boom")
            }

            state is TestState.Waiting && input is Message -> {
                transition {
                    newState = TestState.Confirming(value = input.text.length)
                    add(TestEffect("confirm-prompt"))
                }
            }

            state is TestState.Confirming && input is Callback && input.data == "confirm" -> {
                transition {
                    newState = TestState.Done(value = state.value)
                    add(TestEffect("done"))
                }
            }

            else -> {
                noTransition(state)
            }
        }

    fun postDirect(
        key: ConversationKey,
        reducer: (TestState) -> TransitionResult<TestState>,
    ) = transitionGate.post(key, reducer)
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

private sealed interface PhotoFlowState : State {
    data object AwaitingPhoto : PhotoFlowState

    data class Received(
        val fileId: String,
    ) : PhotoFlowState
}

/**
 * B-03's acceptance, and the whole point is what is NOT in it: no ktgbotapi type, no
 * kotlin-telegram-bot type, no transport import at all. A wizard step asks for a photo and reads
 * the file off telek's own [Photo].
 */
private class PhotoWizardDispatcher : StateDispatcher<PhotoFlowState>() {
    override val startCommand = "passport"
    override val stateClass = PhotoFlowState::class

    override fun entry(input: Input): TransitionResult<PhotoFlowState>? =
        if (input is Message && input.text == "/passport") {
            transition {
                newState = PhotoFlowState.AwaitingPhoto
                add(TestEffect("ask-for-photo"))
            }
        } else {
            null
        }

    override fun transition(
        state: PhotoFlowState,
        input: Input,
    ): TransitionResult<PhotoFlowState> =
        when {
            state is PhotoFlowState.AwaitingPhoto && input is Photo -> {
                transition {
                    newState = PhotoFlowState.Received(input.file.fileId)
                    add(TestEffect("stored-${input.file.fileId}"))
                }
            }

            else -> {
                noTransition(state)
            }
        }
}

class TelekTest {
    @Test
    fun `onInput runs the dispatcher then transitions state and executes effects`() =
        runTest {
            val executor = FakeEffectExecutor()
            val telek = Telek(scope = this, dispatchers = listOf(WizardDispatcher()), effectExecutor = executor)
            telek.onInput(key = key(1), input = Message(1, "/test"))
            advanceUntilIdle()

            assertEquals(listOf(TestEffect("started")), executor.executed.single())
        }

    @Test
    fun `full README-style scenario transitions Waiting to Confirming to Done`() =
        runTest {
            val executor = FakeEffectExecutor()
            val telek = Telek(scope = this, dispatchers = listOf(WizardDispatcher()), effectExecutor = executor)
            telek.onInput(key(1), Message(1, "/test"))
            advanceUntilIdle()
            telek.onInput(key(1), Message(1, "hello"))
            advanceUntilIdle()
            telek.onInput(key(1), Callback(1, messageId = 1, data = "confirm"))
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
    fun `two members of one group run the same wizard without seeing each other's answers`() =
        runTest {
            // The group's address is one number; the two members are two keys under it. Before
            // B-01 this test could not be written: both inputs were filed under the chat, so the
            // second member's message advanced the first member's flow.
            val group = -100L
            val alice = ConversationKey.chatAndUser(chatId = group, userId = 11)
            val bob = ConversationKey.chatAndUser(chatId = group, userId = 22)
            val store = DefaultUserStateStore()
            val telek =
                Telek(
                    scope = this,
                    userStateStore = store,
                    dispatchers = listOf(WizardDispatcher()),
                    effectExecutor = FakeEffectExecutor(),
                )

            telek.onInput(alice, Message(group, "/test"))
            telek.onInput(bob, Message(group, "/test"))
            advanceUntilIdle()

            // Interleaved, and of different lengths, so a shared state would show up as one of
            // them holding the other's value.
            telek.onInput(alice, Message(group, "alice"))
            telek.onInput(bob, Message(group, "bob's longer answer"))
            advanceUntilIdle()

            assertEquals(TestState.Confirming("alice".length), store.get(alice))
            assertEquals(TestState.Confirming("bob's longer answer".length), store.get(bob))

            // And finishing one leaves the other exactly where it was.
            telek.onInput(alice, Callback(group, messageId = 1, data = "confirm"))
            advanceUntilIdle()

            assertNull(store.get(alice))
            assertEquals(TestState.Confirming("bob's longer answer".length), store.get(bob))
        }

    @Test
    fun `a chat-only key keeps one state for the whole chat`() =
        runTest {
            // The opposite arrangement, and the reason ConversationKey.userId is nullable: a bot
            // that wants shared state in a group asks for it and gets exactly the old behaviour.
            val group = ConversationKey.chat(-100)
            val store = DefaultUserStateStore()
            val telek =
                Telek(
                    scope = this,
                    userStateStore = store,
                    dispatchers = listOf(WizardDispatcher()),
                    effectExecutor = FakeEffectExecutor(),
                )

            telek.onInput(group, Message(-100, "/test"))
            advanceUntilIdle()
            telek.onInput(group, Message(-100, "whoever"))
            advanceUntilIdle()

            assertEquals(TestState.Confirming("whoever".length), store.get(group))
        }

    @Test
    fun `a wizard step asks for a photo and receives one`() =
        runTest {
            val executor = FakeEffectExecutor()
            val store = DefaultUserStateStore()
            val telek =
                Telek(
                    scope = this,
                    userStateStore = store,
                    dispatchers = listOf(PhotoWizardDispatcher()),
                    effectExecutor = executor,
                )

            telek.onInput(key(1), Message(1, "/passport"))
            advanceUntilIdle()
            assertEquals(PhotoFlowState.AwaitingPhoto, store.get(key(1)))

            telek.onInput(key(1), Photo(chatId = 1, messageId = 9, file = FileRef("scan-1")))
            advanceUntilIdle()

            assertEquals(PhotoFlowState.Received("scan-1"), store.get(key(1)))
            assertEquals(
                listOf<List<Effect>>(listOf(TestEffect("ask-for-photo")), listOf(TestEffect("stored-scan-1"))),
                executor.executed,
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
            telek.onInput(key(1), Message(1, "/test"))
            advanceUntilIdle()
            telek.onInput(key(1), Message(1, "hello"))
            advanceUntilIdle()
            telek.onInput(key(1), Callback(1, messageId = 1, data = "confirm"))
            advanceUntilIdle()

            assertNull(store.get(key(1)))
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
            telek.onInput(key(1), input)
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
            telek.onInput(key(1), Message(1, "/test"))
            advanceUntilIdle()

            telek.onInput(key(1), Message(1, "boom"))
            advanceUntilIdle()

            assertEquals(1, interceptor.errors.size)
            val error = interceptor.errors.single()
            assertEquals(key(1), error.key)
            assertIs<RuntimeException>(error.error)
            assertEquals("boom", error.error.message)
            assertEquals(TestState.Waiting(0), store.get(key(1)))
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
            dispatcher.postDirect(key(1)) { state -> noTransition(state) }
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
            telek.onInput(key(1), Message(1, "/test"))
            advanceUntilIdle()

            dispatcher.postDirect(key(1)) { state ->
                transition { newState = TestState.Confirming(value = 42) }
            }
            advanceUntilIdle()

            assertEquals(TestState.Confirming(42), store.get(key(1)))
        }

    @Test
    fun `dispatcher onEffectResults receives the effect execution results`() =
        runTest {
            val results = mutableListOf<Pair<State, List<EffectOutcome>>>()
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
                        outcomes: List<EffectOutcome>,
                    ) {
                        results += state to outcomes
                    }
                }
            val executor = FakeEffectExecutor { EffectSuccess }
            val telek = Telek(scope = this, dispatchers = listOf(dispatcher), effectExecutor = executor)
            telek.onInput(key(1), Message(1, "/test"))
            advanceUntilIdle()

            assertEquals(1, results.size)
            assertEquals(TestState.Waiting(0), results.single().first)
            assertTrue(results.single().second.all { it.result === EffectSuccess })
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
            telek.onInput(key(1), input)
            advanceUntilIdle()

            assertEquals(1, interceptor.errors.size)
            val error = interceptor.errors.single()
            assertEquals(key(1), error.key)
            assertEquals(input, error.input)
            assertSame(boom, error.error)
            // The transition itself is unaffected — a failed effect doesn't undo the state change.
            assertEquals(1, interceptor.afterStateChanged.size)
        }

    @Test
    fun `an async effect's event re-enters the FSM through the event overload of transition`() =
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

            telek.onInput(key(1), Message(1, "/async"))
            advanceUntilIdle()

            assertEquals(TestState.Confirming(99), store.get(key(1)))
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

            telek.onInput(key(1), Message(1, "/async"))
            advanceUntilIdle()

            assertEquals(TestState.Waiting(0), store.get(key(1)))
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

            telek.onInput(key(1), Message(1, "/async"))
            advanceUntilIdle()

            assertEquals(TestState.Waiting(0), store.get(key(1)))
            // Only the entry transition — no second, event-triggered one.
            assertEquals(1, interceptor.afterStateChanged.size)
        }
}
