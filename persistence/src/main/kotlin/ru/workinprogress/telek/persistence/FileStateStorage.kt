package ru.workinprogress.telek.persistence

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import ru.workinprogress.telek.State
import ru.workinprogress.telek.StateStorage
import java.io.File
import kotlin.io.readText
import kotlin.io.writeText
import kotlin.onFailure
import kotlin.runCatching

inline fun <reified T : State> stateStorageOf(dir: File = File("./state")): FileStateStorage<T> = FileStateStorage(dir, serializer())

open class FileStateStorage<T : State>(
    private val dir: File,
    private val serializer: KSerializer<T>,
) : StateStorage<T> {
    override suspend fun save(
        chatId: Long,
        state: T,
    ): Unit =
        withContext(Dispatchers.IO) {
            runCatching {
                File(dir, "$chatId.json").writeText(json.encodeToString(serializer, state))
            }.onFailure {
                println("❌ Failed to save $chatId: ${it.message}")
            }
        }

    override suspend fun load(chatId: Long): T? =
        withContext(Dispatchers.IO) {
            runCatching {
                val file = File(dir, "$chatId.json")
                if (!file.exists()) return@withContext null
                json.decodeFromString(serializer, file.readText())
            }.onFailure {
                println("❌ Failed to load $chatId: ${it.message}")
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
