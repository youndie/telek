package io.github.youndie.telek.persistence

import io.github.youndie.telek.ConversationKey
import io.github.youndie.telek.State
import io.github.youndie.telek.StateStorage
import io.github.youndie.telek.TelekLogger
import io.github.youndie.telek.telekIoDispatcher
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath

public inline fun <reified T : State> stateStorageOf(
    dir: Path = "./state".toPath(),
    logger: TelekLogger = TelekLogger.NoOp,
    fileSystem: FileSystem = systemFileSystem,
): FileStateStorage<T> = FileStateStorage(dir, serializer(), logger, fileSystem)

/**
 * Saves/loads one JSON file per [ConversationKey], named by its
 * [storageId][ConversationKey.storageId] — `<chatId>.json` for a key that is a whole chat,
 * `<chatId>.<userId>.json` for one person within a chat. Writes are atomic — [save] writes to a
 * `.tmp` file and renames it into place, so a crash mid-write can never leave a truncated,
 * unreadable file behind; the previous (or no) file is what a concurrent [load] would see instead.
 *
 * File access goes through okio rather than `java.io`/`java.nio` so this works on every telek
 * target. [fileSystem] defaults to the real one; pass okio's `FakeFileSystem` in tests.
 */
public open class FileStateStorage<T : State>(
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
        key: ConversationKey,
        state: T,
    ): Unit =
        withContext(telekIoDispatcher) {
            @Suppress(
                "ktlint:kapkan:cancellation-swallowed",
                "file IO with no suspension point inside, so no cancellation can arrive here",
            )
            runCatching {
                val tmp = dir / "${key.storageId}.json.tmp"
                val target = dir / "${key.storageId}.json"
                fileSystem.write(tmp) { writeUtf8(json.encodeToString(serializer, state)) }
                fileSystem.atomicMove(tmp, target)
            }.onFailure {
                logger.error("Failed to save $key: ${it.message}", it)
            }.getOrThrow()
        }

    override suspend fun load(key: ConversationKey): T? =
        withContext(telekIoDispatcher) {
            @Suppress(
                "ktlint:kapkan:cancellation-swallowed",
                "file IO with no suspension point inside, so getOrNull has no cancellation to hide",
            )
            runCatching {
                val file = dir / "${key.storageId}.json"
                if (!fileSystem.exists(file)) {
                    warnIfSupersededFileExists(key)
                    return@withContext null
                }
                json.decodeFromString(serializer, fileSystem.read(file) { readUtf8() })
            }.onFailure {
                logger.error("Failed to load $key: ${it.message}", it)
            }.getOrNull()
        }

    /**
     * The one moment at which an upgrade is visible, so it is the one moment worth speaking at.
     *
     * Before conversations were keyed by [ConversationKey] they were keyed by the chat alone, and
     * that is still exactly what a chat key writes — `<chatId>.json`. A bot that upgrades and takes
     * the new per-user default therefore asks for `<chatId>.<userId>.json`, does not find it, and
     * gets `null` — which is indistinguishable from a first-time user, because at the level of this
     * interface it *is* `null` either way. That silence is the whole defect: a person mid-wizard
     * looks to the bot like someone who has never written to it.
     *
     * So: nothing is migrated, nothing is deleted, and the file that would have been loaded under
     * the old key is named out loud once. Costs one `exists` on a path that is normally absent, and
     * only for keys that carry a user.
     */
    private fun warnIfSupersededFileExists(key: ConversationKey) {
        if (key.userId == null) return

        val superseded = dir / "${ConversationKey.chat(key.chatId).storageId}.json"
        if (!fileSystem.exists(superseded)) return

        logger.warn(
            "No stored state for $key, but $superseded exists. That file was written when a " +
                "conversation was keyed by its chat alone; it is NOT loaded and NOT deleted, and " +
                "this conversation starts empty. Pass Keying.PerChat to go on reading it, or " +
                "remove it once the flow it holds no longer matters.",
        )
    }

    override suspend fun delete(key: ConversationKey) {
        withContext(telekIoDispatcher) {
            fileSystem.delete(dir / "${key.storageId}.json", mustExist = false)
        }
    }

    public companion object {
        public val json: Json =
            Json {
                prettyPrint = true
                ignoreUnknownKeys = true
                classDiscriminator = "state_type"
            }
    }
}
