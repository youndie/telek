package ru.workinprogress.telek.persistence

import kotlinx.coroutines.test.runTest
import kotlinx.serialization.KSerializer
import kotlinx.serialization.serializer
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem
import ru.workinprogress.telek.State
import ru.workinprogress.telek.UpdateResult
import ru.workinprogress.telek.UserStateStore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

// PersistableUserStateStoreImpl intentionally does no per-chat locking of its own — see
// UserStateStore's KDoc. Telek's ChatWorkers guarantees at most one `update` call per chatId is
// ever in flight; that guarantee is exercised in ChatWorkersTest and TelekTest, not against this
// store in isolation.

private class CountingFileStateStorage<T : State>(
    dir: Path,
    serializer: KSerializer<T>,
    fileSystem: FileSystem,
) : FileStateStorage<T>(dir, serializer, fileSystem = fileSystem) {
    var loadCount = 0

    override suspend fun load(chatId: Long): T? {
        loadCount++
        return super.load(chatId)
    }
}

private inline fun <reified T : State> countingStateStorageOf(
    dir: Path,
    fileSystem: FileSystem,
): CountingFileStateStorage<T> = CountingFileStateStorage(dir, serializer(), fileSystem)

class PersistableUserStateStoreImplTest {
    private val fs = FakeFileSystem()
    private val dir: Path = "/state".toPath()

    private fun storage() = stateStorageOf<PersistenceTestState>(dir, fileSystem = fs)

    private fun updateResult(
        old: State?,
        new: State,
    ) = UpdateResult(oldState = old, newState = new, effects = emptyList(), dispatcher = null)

    @Test
    fun `get loads from storage into cache and does not re-read the file`() =
        runTest {
            val fileStorage = countingStateStorageOf<PersistenceTestState>(dir, fs)
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
            val fileStorage = storage()
            val store = PersistableUserStateStoreImpl(fileStorage)
            val newState = PersistenceTestState.Waiting(5)

            store.update(1) { current -> updateResult(current, newState) }

            assertEquals(newState, store.get(1))
            assertEquals(newState, fileStorage.load(1))
        }

    @Test
    fun `update with a FinalState deletes from storage and memory`() =
        runTest {
            val fileStorage = storage()
            val store = PersistableUserStateStoreImpl(fileStorage)
            store.update(1) { current -> updateResult(current, PersistenceTestState.Waiting(0)) }

            store.update(1) { current -> updateResult(current, PersistenceTestState.Done(1)) }

            assertNull(store.get(1))
            assertNull(fileStorage.load(1))
        }

    @Test
    fun `clear removes state from storage and memory`() =
        runTest {
            val fileStorage = storage()
            val store = PersistableUserStateStoreImpl(fileStorage)
            store.update(1) { current -> updateResult(current, PersistenceTestState.Waiting(0)) }

            store.clear(1)

            assertNull(store.get(1))
            assertNull(fileStorage.load(1))
        }

    @Test
    fun `PersistableUserStateStoreImpl is a drop-in UserStateStore implementation`() =
        runTest {
            val store: UserStateStore = PersistableUserStateStoreImpl(storage())

            store.update(1) { current -> updateResult(current, PersistenceTestState.Waiting(1)) }
            assertEquals(PersistenceTestState.Waiting(1), store.get(1))

            store.clear(1)
            assertNull(store.get(1))
        }

    @Test
    fun `state saved before a restart is visible to a fresh store over the same directory`() =
        runTest {
            val firstStore = PersistableUserStateStoreImpl(storage())
            firstStore.update(1) { current -> updateResult(current, PersistenceTestState.Waiting(9)) }

            val restartedStore = PersistableUserStateStoreImpl(storage())

            assertEquals(PersistenceTestState.Waiting(9), restartedStore.get(1))
        }
}
