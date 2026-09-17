package io.github.youndie.telek

import io.github.youndie.telek.support.OtherState
import io.github.youndie.telek.support.SimpleDispatcher
import io.github.youndie.telek.support.TestState
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
    fun `callback input is routed by current state type rather than command prefix`() {
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

    // B-03. The four media inputs and a bot's own type all take the same path: there is nothing in
    // them that could start a flow, so they belong to whatever state is already there.
    @Test
    fun `a photo is routed by current state type`() {
        val strategy = DefaultFindDispatcherStrategy(listOf(exampleDispatcher, otherDispatcher))

        val found =
            strategy.findDispatcher(
                state = TestState.Waiting(),
                input = Photo(1, messageId = 5, file = FileRef("file-id")),
            )

        assertSame(exampleDispatcher, found)
    }

    @Test
    fun `a document a contact and a location are routed by current state type`() {
        val strategy = DefaultFindDispatcherStrategy(listOf(exampleDispatcher, otherDispatcher))
        val inputs =
            listOf(
                Document(1, messageId = 5, file = FileRef("file-id")),
                Contact(1, messageId = 5, phoneNumber = "+70000000000", firstName = "A"),
                Location(1, messageId = 5, latitude = 1.0, longitude = 2.0),
            )

        inputs.forEach { input ->
            assertSame(exampleDispatcher, strategy.findDispatcher(TestState.Waiting(), input), input.toString())
        }
    }

    // The additivity criterion, shown rather than asserted: this type is declared HERE, not in
    // :core, and it reaches a dispatcher without a line of :core knowing it exists.
    @Test
    fun `an Input a bot declared itself is routed by current state type`() {
        val strategy = DefaultFindDispatcherStrategy(listOf(exampleDispatcher, otherDispatcher))

        val found = strategy.findDispatcher(TestState.Waiting(), ConsumerDefinedInput(chatId = 1))

        assertSame(exampleDispatcher, found)
    }

    private data class ConsumerDefinedInput(
        override val chatId: Long,
    ) : Input
}
