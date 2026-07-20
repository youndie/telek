package ru.workinprogress.telek.persistence

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FileStateStorageTest {
    @TempDir
    lateinit var dir: File

    @Test
    fun `save then load round-trips the same state`() =
        runTest {
            val storage = stateStorageOf<PersistenceTestState>(dir)
            val state = PersistenceTestState.Waiting(value = 7)

            storage.save(chatId = 1, state = state)
            val loaded = storage.load(chatId = 1)

            assertEquals(state, loaded)
        }

    @Test
    fun `each chatId is saved to its own file`() =
        runTest {
            val storage = stateStorageOf<PersistenceTestState>(dir)

            storage.save(1, PersistenceTestState.Waiting(1))
            storage.save(2, PersistenceTestState.Waiting(2))

            assertTrue(File(dir, "1.json").exists())
            assertTrue(File(dir, "2.json").exists())
            assertEquals(PersistenceTestState.Waiting(1), storage.load(1))
            assertEquals(PersistenceTestState.Waiting(2), storage.load(2))
        }

    @Test
    fun `load of unknown chatId returns null`() =
        runTest {
            val storage = stateStorageOf<PersistenceTestState>(dir)

            assertNull(storage.load(chatId = 999))
        }

    @Test
    fun `load of corrupted json returns null instead of throwing`() =
        runTest {
            val storage = stateStorageOf<PersistenceTestState>(dir)
            File(dir, "5.json").writeText("{ not valid json ")

            val loaded = storage.load(chatId = 5)

            assertNull(loaded)
        }

    @Test
    fun `delete removes the file so a subsequent load returns null`() =
        runTest {
            val storage = stateStorageOf<PersistenceTestState>(dir)
            storage.save(1, PersistenceTestState.Waiting(1))

            storage.delete(1)

            assertFalse(File(dir, "1.json").exists())
            assertNull(storage.load(1))
        }

    @Test
    fun `delete of a non-existent file does not throw`() =
        runTest {
            val storage = stateStorageOf<PersistenceTestState>(dir)

            storage.delete(chatId = 42)
        }

    @Test
    fun `stateStorageOf creates a working storage with a valid serializer`() =
        runTest {
            val storage = stateStorageOf<PersistenceTestState>(dir)
            val state = PersistenceTestState.Done(value = 3)

            storage.save(1, state)

            assertEquals(state, storage.load(1))
        }

    @Test
    fun `ignoreUnknownKeys allows loading json with extra fields`() =
        runTest {
            val storage = stateStorageOf<PersistenceTestState>(dir)
            File(dir, "1.json").writeText(
                """
                {
                    "state_type": "ru.workinprogress.telek.persistence.PersistenceTestState.Waiting",
                    "value": 4,
                    "unexpectedExtraField": "ignored"
                }
                """.trimIndent(),
            )

            val loaded = storage.load(1)

            assertEquals(PersistenceTestState.Waiting(4), loaded)
        }

    @Test
    fun `saved json uses state_type as the class discriminator`() =
        runTest {
            val storage = stateStorageOf<PersistenceTestState>(dir)

            storage.save(1, PersistenceTestState.Waiting(1))

            val raw = File(dir, "1.json").readText()
            assertTrue(raw.contains("\"state_type\""))
        }

    @Test
    fun `the storage directory is created if it doesn't exist yet`() =
        runTest {
            val missingDir = File(dir, "nested/does/not/exist")
            val storage = stateStorageOf<PersistenceTestState>(missingDir)

            storage.save(1, PersistenceTestState.Waiting(1))

            assertTrue(File(missingDir, "1.json").exists())
        }

    @Test
    fun `save does not leave a tmp file behind on success`() =
        runTest {
            val storage = stateStorageOf<PersistenceTestState>(dir)

            storage.save(1, PersistenceTestState.Waiting(1))

            assertFalse(File(dir, "1.json.tmp").exists())
        }

    @Test
    fun `a leftover tmp file from a crashed save doesn't affect loading the real file`() =
        runTest {
            val storage = stateStorageOf<PersistenceTestState>(dir)
            storage.save(1, PersistenceTestState.Waiting(1))
            // Simulates a process that died mid-write, after writing the tmp file but before the
            // atomic rename — the previous save's target file is untouched.
            File(dir, "1.json.tmp").writeText("{ this would be corrupted if it were ever read ")

            val loaded = storage.load(1)

            assertEquals(PersistenceTestState.Waiting(1), loaded)
        }

    @Test
    fun `save throws instead of silently swallowing a write failure`() =
        runTest {
            val notADirectory = File(dir, "actually-a-file")
            notADirectory.writeText("occupying this path")
            val storage = stateStorageOf<PersistenceTestState>(notADirectory)

            assertFailsWith<java.io.IOException> {
                storage.save(1, PersistenceTestState.Waiting(1))
            }
        }
}
