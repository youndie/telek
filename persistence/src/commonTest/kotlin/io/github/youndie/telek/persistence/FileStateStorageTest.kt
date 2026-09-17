package io.github.youndie.telek.persistence

import io.github.youndie.telek.ConversationKey
import io.github.youndie.telek.TelekLogLevel
import io.github.youndie.telek.TelekLogger
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

    private fun storage(
        at: Path = dir,
        logger: TelekLogger = TelekLogger.NoOp,
    ) = stateStorageOf<PersistenceTestState>(at, logger = logger, fileSystem = fs)

    /** Records warnings only. `log` is the interface's one abstract member; warn/error delegate to it. */
    private fun recording(into: MutableList<String>) =
        object : TelekLogger {
            override fun log(
                level: TelekLogLevel,
                message: String,
                error: Throwable?,
            ) {
                if (level == TelekLogLevel.WARN) into += message
            }
        }

    private fun writeRaw(
        path: Path,
        content: String,
    ) = fs.write(path) { writeUtf8(content) }

    @Test
    fun `save then load round-trips the same state`() =
        runTest {
            val storage = storage()
            val state = PersistenceTestState.Waiting(value = 7)

            storage.save(key = k(1), state = state)
            val loaded = storage.load(key = k(1))

            assertEquals(state, loaded)
        }

    @Test
    fun `each chatId is saved to its own file`() =
        runTest {
            val storage = storage()

            storage.save(k(1), PersistenceTestState.Waiting(1))
            storage.save(k(2), PersistenceTestState.Waiting(2))

            assertTrue(fs.exists(dir / "1.json"))
            assertTrue(fs.exists(dir / "2.json"))
            assertEquals(PersistenceTestState.Waiting(1), storage.load(k(1)))
            assertEquals(PersistenceTestState.Waiting(2), storage.load(k(2)))
        }

    @Test
    fun `two members of one chat get two files and a chat key keeps the old bare name`() =
        runTest {
            val storage = storage()
            val group = -100L

            storage.save(ConversationKey.chatAndUser(group, 11), PersistenceTestState.Waiting(11))
            storage.save(ConversationKey.chatAndUser(group, 22), PersistenceTestState.Waiting(22))
            storage.save(ConversationKey.chat(group), PersistenceTestState.Waiting(0))

            // The bare name is what every version before the key change wrote, so state stored by
            // one of those is still found — under a chat key, and only under a chat key.
            assertTrue(fs.exists(dir / "-100.json"))
            assertTrue(fs.exists(dir / "-100.11.json"))
            assertTrue(fs.exists(dir / "-100.22.json"))
            assertEquals(
                PersistenceTestState.Waiting(11),
                storage.load(ConversationKey.chatAndUser(group, 11)),
            )
            assertEquals(PersistenceTestState.Waiting(0), storage.load(ConversationKey.chat(group)))
        }

    @Test
    fun `deleting one member's state leaves the other's alone`() =
        runTest {
            val storage = storage()
            val alice = ConversationKey.chatAndUser(-100, 11)
            val bob = ConversationKey.chatAndUser(-100, 22)
            storage.save(alice, PersistenceTestState.Waiting(11))
            storage.save(bob, PersistenceTestState.Waiting(22))

            storage.delete(alice)

            assertNull(storage.load(alice))
            assertEquals(PersistenceTestState.Waiting(22), storage.load(bob))
        }

    // B-02: the upgrade case. A file written before conversations were keyed by ConversationKey is
    // named after the chat alone, which is what a chat key still writes — so under the per-user
    // default it is simply not found. `null` is correct; being quiet about it is not.
    @Test
    fun `a superseded chat-keyed file is not loaded under a per-user key - and is named in a warning`() =
        runTest {
            val warnings = mutableListOf<String>()
            val storage = storage(logger = recording(warnings))
            storage.save(ConversationKey.chat(-100), PersistenceTestState.Waiting(7))

            val loaded = storage.load(ConversationKey.chatAndUser(-100, 11))

            assertNull(loaded)
            assertEquals(1, warnings.size)
            assertTrue(warnings.single().contains("-100.json"), warnings.single())
        }

    @Test
    fun `the superseded file is left on disk and still reads back under a chat key`() =
        runTest {
            val storage = storage()
            storage.save(ConversationKey.chat(-100), PersistenceTestState.Waiting(7))

            storage.load(ConversationKey.chatAndUser(-100, 11))

            // Not migrated and not deleted: the documented fallback has to actually work.
            assertTrue(fs.exists(dir / "-100.json"))
            assertEquals(PersistenceTestState.Waiting(7), storage.load(ConversationKey.chat(-100)))
        }

    // The control. Without it the assertion above passes just as well for a storage that warns on
    // every miss, which would make the warning noise rather than a signal.
    @Test
    fun `a genuinely new conversation is silent`() =
        runTest {
            val warnings = mutableListOf<String>()
            val storage = storage(logger = recording(warnings))

            assertNull(storage.load(ConversationKey.chatAndUser(-100, 11)))

            assertEquals(emptyList(), warnings)
        }

    @Test
    fun `a miss on a chat key is silent - because that key is the old shape itself`() =
        runTest {
            val warnings = mutableListOf<String>()
            val storage = storage(logger = recording(warnings))
            storage.save(ConversationKey.chat(-100), PersistenceTestState.Waiting(7))

            assertNull(storage.load(ConversationKey.chat(-999)))

            assertEquals(emptyList(), warnings)
        }

    @Test
    fun `load of unknown chatId returns null`() =
        runTest {
            assertNull(storage().load(key = k(999)))
        }

    @Test
    fun `load of corrupted json returns null instead of throwing`() =
        runTest {
            val storage = storage()
            writeRaw(dir / "5.json", "{ not valid json ")

            assertNull(storage.load(key = k(5)))
        }

    @Test
    fun `delete removes the file so a subsequent load returns null`() =
        runTest {
            val storage = storage()
            storage.save(k(1), PersistenceTestState.Waiting(1))

            storage.delete(k(1))

            assertFalse(fs.exists(dir / "1.json"))
            assertNull(storage.load(k(1)))
        }

    @Test
    fun `delete of a non-existent file does not throw`() =
        runTest {
            storage().delete(key = k(42))
        }

    @Test
    fun `stateStorageOf creates a working storage with a valid serializer`() =
        runTest {
            val storage = storage()
            val state = PersistenceTestState.Done(value = 3)

            storage.save(k(1), state)

            assertEquals(state, storage.load(k(1)))
        }

    @Test
    fun `ignoreUnknownKeys allows loading json with extra fields`() =
        runTest {
            val storage = storage()
            writeRaw(
                dir / "1.json",
                """
                {
                    "state_type": "io.github.youndie.telek.persistence.PersistenceTestState.Waiting",
                    "value": 4,
                    "unexpectedExtraField": "ignored"
                }
                """.trimIndent(),
            )

            assertEquals(PersistenceTestState.Waiting(4), storage.load(k(1)))
        }

    @Test
    fun `saved json uses state_type as the class discriminator`() =
        runTest {
            val storage = storage()

            storage.save(k(1), PersistenceTestState.Waiting(1))

            val raw = fs.read(dir / "1.json") { readUtf8() }
            assertTrue(raw.contains("\"state_type\""))
        }

    @Test
    fun `the storage directory is created if it doesn't exist yet`() =
        runTest {
            val missingDir = dir / "nested/does/not/exist"
            val storage = storage(missingDir)

            storage.save(k(1), PersistenceTestState.Waiting(1))

            assertTrue(fs.exists(missingDir / "1.json"))
        }

    @Test
    fun `save does not leave a tmp file behind on success`() =
        runTest {
            val storage = storage()

            storage.save(k(1), PersistenceTestState.Waiting(1))

            assertFalse(fs.exists(dir / "1.json.tmp"))
        }

    @Test
    fun `a leftover tmp file from a crashed save doesn't affect loading the real file`() =
        runTest {
            val storage = storage()
            storage.save(k(1), PersistenceTestState.Waiting(1))
            // Simulates a process that died mid-write, after writing the tmp file but before the
            // atomic rename — the previous save's target file is untouched.
            writeRaw(dir / "1.json.tmp", "{ this would be corrupted if it were ever read ")

            assertEquals(PersistenceTestState.Waiting(1), storage.load(k(1)))
        }

    @Test
    fun `save throws instead of silently swallowing a write failure`(): Unit =
        runTest {
            val storage = storage()
            // Occupy the path save() writes to with a directory, so the write itself fails.
            fs.createDirectories(dir / "1.json.tmp")

            assertFailsWith<IOException> {
                storage.save(k(1), PersistenceTestState.Waiting(1))
            }
        }
}
