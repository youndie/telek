package ru.workinprogress.telek.persistence

import kotlinx.coroutines.test.runTest
import okio.IOException
import okio.Path
import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Runs against okio's [FakeFileSystem] rather than a real temp directory, so the same tests run on
 * every target instead of needing JUnit's JVM-only `@TempDir`.
 */
class FileStateStorageTest {
    private val fs = FakeFileSystem()
    private val dir: Path = "/state".toPath()

    private fun storage(at: Path = dir) = stateStorageOf<PersistenceTestState>(at, fileSystem = fs)

    private fun writeRaw(
        path: Path,
        content: String,
    ) = fs.write(path) { writeUtf8(content) }

    @Test
    fun `save then load round-trips the same state`() =
        runTest {
            val storage = storage()
            val state = PersistenceTestState.Waiting(value = 7)

            storage.save(chatId = 1, state = state)
            val loaded = storage.load(chatId = 1)

            assertEquals(state, loaded)
        }

    @Test
    fun `each chatId is saved to its own file`() =
        runTest {
            val storage = storage()

            storage.save(1, PersistenceTestState.Waiting(1))
            storage.save(2, PersistenceTestState.Waiting(2))

            assertTrue(fs.exists(dir / "1.json"))
            assertTrue(fs.exists(dir / "2.json"))
            assertEquals(PersistenceTestState.Waiting(1), storage.load(1))
            assertEquals(PersistenceTestState.Waiting(2), storage.load(2))
        }

    @Test
    fun `load of unknown chatId returns null`() =
        runTest {
            assertNull(storage().load(chatId = 999))
        }

    @Test
    fun `load of corrupted json returns null instead of throwing`() =
        runTest {
            val storage = storage()
            writeRaw(dir / "5.json", "{ not valid json ")

            assertNull(storage.load(chatId = 5))
        }

    @Test
    fun `delete removes the file so a subsequent load returns null`() =
        runTest {
            val storage = storage()
            storage.save(1, PersistenceTestState.Waiting(1))

            storage.delete(1)

            assertFalse(fs.exists(dir / "1.json"))
            assertNull(storage.load(1))
        }

    @Test
    fun `delete of a non-existent file does not throw`() =
        runTest {
            storage().delete(chatId = 42)
        }

    @Test
    fun `stateStorageOf creates a working storage with a valid serializer`() =
        runTest {
            val storage = storage()
            val state = PersistenceTestState.Done(value = 3)

            storage.save(1, state)

            assertEquals(state, storage.load(1))
        }

    @Test
    fun `ignoreUnknownKeys allows loading json with extra fields`() =
        runTest {
            val storage = storage()
            writeRaw(
                dir / "1.json",
                """
                {
                    "state_type": "ru.workinprogress.telek.persistence.PersistenceTestState.Waiting",
                    "value": 4,
                    "unexpectedExtraField": "ignored"
                }
                """.trimIndent(),
            )

            assertEquals(PersistenceTestState.Waiting(4), storage.load(1))
        }

    @Test
    fun `saved json uses state_type as the class discriminator`() =
        runTest {
            val storage = storage()

            storage.save(1, PersistenceTestState.Waiting(1))

            val raw = fs.read(dir / "1.json") { readUtf8() }
            assertTrue(raw.contains("\"state_type\""))
        }

    @Test
    fun `the storage directory is created if it doesn't exist yet`() =
        runTest {
            val missingDir = dir / "nested/does/not/exist"
            val storage = storage(missingDir)

            storage.save(1, PersistenceTestState.Waiting(1))

            assertTrue(fs.exists(missingDir / "1.json"))
        }

    @Test
    fun `save does not leave a tmp file behind on success`() =
        runTest {
            val storage = storage()

            storage.save(1, PersistenceTestState.Waiting(1))

            assertFalse(fs.exists(dir / "1.json.tmp"))
        }

    @Test
    fun `a leftover tmp file from a crashed save doesn't affect loading the real file`() =
        runTest {
            val storage = storage()
            storage.save(1, PersistenceTestState.Waiting(1))
            // Simulates a process that died mid-write, after writing the tmp file but before the
            // atomic rename — the previous save's target file is untouched.
            writeRaw(dir / "1.json.tmp", "{ this would be corrupted if it were ever read ")

            assertEquals(PersistenceTestState.Waiting(1), storage.load(1))
        }

    @Test
    fun `save throws instead of silently swallowing a write failure`(): Unit =
        runTest {
            val storage = storage()
            // Occupy the path save() writes to with a directory, so the write itself fails.
            fs.createDirectories(dir / "1.json.tmp")

            assertFailsWith<IOException> {
                storage.save(1, PersistenceTestState.Waiting(1))
            }
        }
}
