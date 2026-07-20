package ru.workinprogress.telek.persistence

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import ru.workinprogress.telek.State
import ru.workinprogress.telek.StateStorage
import ru.workinprogress.telek.TelekLogger
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import kotlin.io.readText
import kotlin.onFailure
import kotlin.runCatching

inline fun <reified T : State> stateStorageOf(
    dir: File = File("./state"),
    logger: TelekLogger = TelekLogger.NoOp,
): FileStateStorage<T> = FileStateStorage(dir, serializer(), logger)

/**
 * Saves/loads one JSON file per `chatId`. Writes are atomic — [save] writes to a `.tmp` file and
 * renames it into place, so a crash mid-write can never leave a truncated, unreadable
 * `$chatId.json` behind; the previous (or no) file is what a concurrent [load] would see instead.
 */
open class FileStateStorage<T : State>(
    private val dir: File,
    private val serializer: KSerializer<T>,
    private val logger: TelekLogger = TelekLogger.NoOp,
) : StateStorage<T> {
    init {
        dir.mkdirs()
    }

    /** Throws if the write fails — a state store must not silently pretend a save succeeded. */
    override suspend fun save(
        chatId: Long,
        state: T,
    ): Unit =
        withContext(Dispatchers.IO) {
            runCatching {
                val tmp = File(dir, "$chatId.json.tmp")
                val target = File(dir, "$chatId.json")
                tmp.writeText(json.encodeToString(serializer, state))
                Files.move(
                    tmp.toPath(),
                    target.toPath(),
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING,
                )
            }.onFailure {
                logger.error("Failed to save $chatId: ${it.message}", it)
            }.getOrThrow()
        }

    override suspend fun load(chatId: Long): T? =
        withContext(Dispatchers.IO) {
            runCatching {
                val file = File(dir, "$chatId.json")
                if (!file.exists()) return@withContext null
                json.decodeFromString(serializer, file.readText())
            }.onFailure {
                logger.error("Failed to load $chatId: ${it.message}", it)
            }.getOrNull()
        }

    override suspend fun delete(chatId: Long) {
        withContext(Dispatchers.IO) {
            File(dir, "$chatId.json").delete()
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
