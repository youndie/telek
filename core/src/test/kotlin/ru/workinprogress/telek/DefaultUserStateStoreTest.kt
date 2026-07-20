package ru.workinprogress.telek

import kotlinx.coroutines.test.runTest
import ru.workinprogress.telek.support.OtherState
import ru.workinprogress.telek.support.TestState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

// DefaultUserStateStore intentionally does no per-chat locking of its own: Telek's ChatWorkers
// guarantees at most one `update` call per chatId is ever in flight (see UserStateStore's KDoc).
// Concurrency correctness for the same chatId is exercised through that actor, in ChatWorkersTest
// and TelekTest — not here against the store in isolation.

class DefaultUserStateStoreTest {
    private fun updateResult(
        old: State?,
        new: State,
    ) = UpdateResult(oldState = old, newState = new, effects = emptyList(), dispatcher = null)

    @Test
    fun `get returns null for unknown chatId`() =
        runTest {
            val store = DefaultUserStateStore()
            assertNull(store.get(chatId = 1))
        }

    @Test
    fun `update persists newState and it is readable via get`() =
        runTest {
            val store = DefaultUserStateStore()
            val newState = TestState.Waiting(value = 1)

            store.update(chatId = 1) { current -> updateResult(current, newState) }

            assertEquals(newState, store.get(chatId = 1))
        }

    @Test
    fun `update with FinalState clears the stored state`() =
        runTest {
            val store = DefaultUserStateStore()
            store.update(chatId = 1) { current -> updateResult(current, TestState.Waiting(0)) }

            store.update(chatId = 1) { current -> updateResult(current, TestState.Done(1)) }

            assertNull(store.get(chatId = 1))
        }

    @Test
    fun `clear removes state`() =
        runTest {
            val store = DefaultUserStateStore()
            store.update(chatId = 1) { current -> updateResult(current, TestState.Waiting(0)) }

            store.clear(chatId = 1)

            assertNull(store.get(chatId = 1))
        }

    @Test
    fun `state is isolated per chatId`() =
        runTest {
            val store = DefaultUserStateStore()
            store.update(chatId = 1) { current -> updateResult(current, TestState.Waiting(1)) }
            store.update(chatId = 2) { current -> updateResult(current, OtherState(2)) }

            assertEquals(TestState.Waiting(1), store.get(chatId = 1))
            assertEquals(OtherState(2), store.get(chatId = 2))
        }
}
