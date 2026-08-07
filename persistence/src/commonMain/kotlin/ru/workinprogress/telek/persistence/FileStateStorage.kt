package ru.workinprogress.telek.persistence

import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import ru.workinprogress.telek.State
import ru.workinprogress.telek.StateStorage
import ru.workinprogress.telek.TelekLogger
import ru.workinprogress.telek.telekIoDispatcher

inline fun <reified T : State> stateStorageOf(
    dir: Path = "./state".toPath(),
    logger: TelekLogger = TelekLogger.NoOp,
    fileSystem: FileSystem = systemFileSystem,
): FileStateStorage<T> = FileStateStorage(dir, serializer(), logger, fileSystem)

/**
 * Saves/loads one JSON file per `chatId`. Writes are atomic — [save] writes to a `.tmp` file and
 * renames it into place, so a crash mid-write can never leave a truncated, unreadable
 * `$chatId.json` behind; the previous (or no) file is what a concurrent [load] would see instead.
 *
 * File access goes through okio rather than `java.io`/`java.nio` so this works on every telek
 * target. [fileSystem] defaults to the real one; pass okio's `FakeFileSystem` in tests.
 */
open class FileStateStorage<T : State>(
    private val dir: Path,
    private val serializer: KSerializer<T>,
    private val logger: TelekLogger = TelekLogger.NoOp,
    private val fileSystem: FileSystem = systemFileSystem,
) : StateStorage<T> {
    init {
        fileSystem.createDirectories(dir)
    }

    /** Throws if the write fails — a state store must not silently pretend a save succeeded. */
    override suspend fun save(
        chatId: Long,
        state: T,
    ): Unit =
        withContext(telekIoDispatcher) {
            runCatching {
                val tmp = dir / "$chatId.json.tmp"
                val target = dir / "$chatId.json"
                fileSystem.write(tmp) { writeUtf8(json.encodeToString(serializer, state)) }
                fileSystem.atomicMove(tmp, target)
            }.onFailure {
                logger.error("Failed to save $chatId: ${it.message}", it)
            }.getOrThrow()
        }

    override suspend fun load(chatId: Long): T? =
        withContext(telekIoDispatcher) {
            runCatching {
                val file = dir / "$chatId.json"
                if (!fileSystem.exists(file)) return@withContext null
                json.decodeFromString(serializer, fileSystem.read(file) { readUtf8() })
            }.onFailure {
                logger.error("Failed to load $chatId: ${it.message}", it)
            }.getOrNull()
        }

    override suspend fun delete(chatId: Long) {
        withContext(telekIoDispatcher) {
            fileSystem.delete(dir / "$chatId.json", mustExist = false)
        }
    }

    companion object {
        val json =
            Json {
                prettyPrint = true
                ignoreUnknownKeys = true
                classDiscriminator = "state_type"
            }
    }
}
