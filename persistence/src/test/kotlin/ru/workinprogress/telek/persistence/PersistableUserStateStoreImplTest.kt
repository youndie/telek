package ru.workinprogress.telek.persistence

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.KSerializer
import kotlinx.serialization.serializer
import org.junit.jupiter.api.io.TempDir
import ru.workinprogress.telek.State
import ru.workinprogress.telek.UpdateResult
import ru.workinprogress.telek.UserStateStore
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

private class CountingFileStateStorage<T : State>(
    dir: File,
    serializer: KSerializer<T>,
) : FileStateStorage<T>(dir, serializer) {
    var loadCount = 0

    override suspend fun load(chatId: Long): T? {
        loadCount++
        return super.load(chatId)
    }
}

private inline fun <reified T : State> countingStateStorageOf(dir: File): CountingFileStateStorage<T> =
    CountingFileStateStorage(dir, serializer())

class PersistableUserStateStoreImplTest {
    @TempDir
    lateinit var dir: File

    private fun updateResult(
        old: State?,
        new: State,
    ) = UpdateResult(oldState = old, newState = new, effects = emptyList(), dispatcher = null)

    @Test
    fun `get loads from storage into cache and does not re-read the file`() =
        runTest {
            val fileStorage = countingStateStorageOf<PersistenceTestState>(dir)
            fileStorage.save(1, PersistenceTestState.Waiting(3))
            val store = PersistableUserStateStoreImpl(fileStorage)

            val first = store.get(1)
            val second = store.get(1)

            assertEquals(PersistenceTestState.Waiting(3), first)
            assertEquals(PersistenceTestState.Waiting(3), second)
            assertEquals(1, fileStorage.loadCount)
        }

    @Test
    fun `update on a non-final state writes to both memory and storage`() =
        runTest {
            val fileStorage = stateStorageOf<PersistenceTestState>(dir)
            val store = PersistableUserStateStoreImpl(fileStorage)
            val newState = PersistenceTestState.Waiting(5)

            store.update(1) { current -> updateResult(current, newState) }

            assertEquals(newState, store.get(1))
            assertEquals(newState, fileStorage.load(1))
        }

    @Test
    fun `update with a FinalState deletes from storage and memory`() =
        runTest {
            val fileStorage = stateStorageOf<PersistenceTestState>(dir)
            val store = PersistableUserStateStoreImpl(fileStorage)
            store.update(1) { current -> updateResult(current, PersistenceTestState.Waiting(0)) }

            store.update(1) { current -> updateResult(current, PersistenceTestState.Done(1)) }

            assertNull(store.get(1))
            assertNull(fileStorage.load(1))
        }

    @Test
    fun `clear removes state from storage and memory`() =
        runTest {
            val fileStorage = stateStorageOf<PersistenceTestState>(dir)
            val store = PersistableUserStateStoreImpl(fileStorage)
            store.update(1) { current -> updateResult(current, PersistenceTestState.Waiting(0)) }

            store.clear(1)

            assertNull(store.get(1))
            assertNull(fileStorage.load(1))
        }

    @Test
    fun `PersistableUserStateStoreImpl is a drop-in UserStateStore implementation`() =
        runTest {
            val fileStorage = stateStorageOf<PersistenceTestState>(dir)
            val store: UserStateStore = PersistableUserStateStoreImpl(fileStorage)

            store.update(1) { current -> updateResult(current, PersistenceTestState.Waiting(1)) }
            assertEquals(PersistenceTestState.Waiting(1), store.get(1))

            store.clear(1)
            assertNull(store.get(1))
        }

    @Test
    fun `state saved before a restart is visible to a fresh store over the same directory`() =
        runTest {
            val firstStore = PersistableUserStateStoreImpl(stateStorageOf<PersistenceTestState>(dir))
            firstStore.update(1) { current -> updateResult(current, PersistenceTestState.Waiting(9)) }

            val restartedStore = PersistableUserStateStoreImpl(stateStorageOf<PersistenceTestState>(dir))

            assertEquals(PersistenceTestState.Waiting(9), restartedStore.get(1))
        }

    @Test
    fun `concurrent updates for the same chatId are serialized`() =
        runTest {
            val fileStorage = stateStorageOf<PersistenceTestState>(dir)
            val store = PersistableUserStateStoreImpl(fileStorage)
            store.update(1) { current -> updateResult(current, PersistenceTestState.Waiting(0)) }

            val jobs =
                (1..50).map {
                    async {
                        store.update(1) { current ->
                            val state = assertIs<PersistenceTestState.Waiting>(current)
                            updateResult(current, state.copy(value = state.value + 1))
                        }
                    }
                }
            jobs.awaitAll()

            assertEquals(PersistenceTestState.Waiting(50), store.get(1))
            assertEquals(PersistenceTestState.Waiting(50), fileStorage.load(1))
        }
}
