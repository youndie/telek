package ru.workinprogress.telek

import ru.workinprogress.telek.support.OtherState
import ru.workinprogress.telek.support.SimpleDispatcher
import ru.workinprogress.telek.support.TestState
import kotlin.test.Test
import kotlin.test.assertNull
import kotlin.test.assertSame

class DefaultFindDispatcherStrategyTest {
    private val exampleDispatcher = SimpleDispatcher("example", TestState::class)
    private val otherDispatcher = SimpleDispatcher("other", OtherState::class)
    private val fallbackDispatcher = SimpleDispatcher("*", OtherState::class)

    @Test
    fun `command matches dispatcher by startCommand`() {
        val strategy = DefaultFindDispatcherStrategy(listOf(exampleDispatcher, otherDispatcher))

        val found = strategy.findDispatcher(state = null, input = Message(1, "/example"))

        assertSame(exampleDispatcher, found)
    }

    @Test
    fun `command with no direct match falls back to wildcard dispatcher`() {
        val strategy = DefaultFindDispatcherStrategy(listOf(exampleDispatcher, fallbackDispatcher))

        val found = strategy.findDispatcher(state = null, input = Message(1, "/unknown"))

        assertSame(fallbackDispatcher, found)
    }

    @Test
    fun `command with no match and no wildcard returns null`() {
        val strategy = DefaultFindDispatcherStrategy(listOf(exampleDispatcher))

        val found = strategy.findDispatcher(state = null, input = Message(1, "/unknown"))

        assertNull(found)
    }

    @Test
    fun `plain message is routed by current state type`() {
        val strategy = DefaultFindDispatcherStrategy(listOf(exampleDispatcher, otherDispatcher))

        val found = strategy.findDispatcher(state = TestState.Waiting(0), input = Message(1, "hello"))

        assertSame(exampleDispatcher, found)
    }

    @Test
    fun `callback input is routed by current state type, not command prefix`() {
        val strategy = DefaultFindDispatcherStrategy(listOf(exampleDispatcher, otherDispatcher))

        val found =
            strategy.findDispatcher(
                state = OtherState(0),
                input = Callback(chatId = 1, messageId = 1, data = "/example"),
            )

        assertSame(otherDispatcher, found)
    }

    @Test
    fun `null state and non-command input returns null`() {
        val strategy = DefaultFindDispatcherStrategy(listOf(exampleDispatcher, otherDispatcher))

        val found = strategy.findDispatcher(state = null, input = Message(1, "hello"))

        assertNull(found)
    }

    @Test
    fun `callback is routed by canHandleCallback when there is no matching state`() {
        val strategy = DefaultFindDispatcherStrategy(listOf(exampleDispatcher, otherDispatcher))

        val found =
            strategy.findDispatcher(
                state = null,
                input = Callback(chatId = 1, messageId = 1, data = "example"),
            )

        assertSame(exampleDispatcher, found)
    }

    @Test
    fun `callback routed by canHandleCallback takes priority over state-based lookup`() {
        val strategy = DefaultFindDispatcherStrategy(listOf(exampleDispatcher, otherDispatcher))

        val found =
            strategy.findDispatcher(
                state = OtherState(0),
                input = Callback(chatId = 1, messageId = 1, data = "example"),
            )

        assertSame(exampleDispatcher, found)
    }

    @Test
    fun `callback matching no dispatcher and no state returns null`() {
        val strategy = DefaultFindDispatcherStrategy(listOf(exampleDispatcher, otherDispatcher))

        val found =
            strategy.findDispatcher(
                state = null,
                input = Callback(chatId = 1, messageId = 1, data = "unrelated"),
            )

        assertNull(found)
    }
}
