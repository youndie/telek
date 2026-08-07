// Compiled copy of README.md's "Persistence module" section — keep both in sync.
package ru.workinprogress.telek.docs

import kotlinx.serialization.Serializable
import okio.Path.Companion.toPath
import ru.workinprogress.telek.State
import ru.workinprogress.telek.Telek
import ru.workinprogress.telek.persistence.PersistableUserStateStoreImpl
import ru.workinprogress.telek.persistence.stateStorageOf
import ru.workinprogress.telek.telegram.TelegramContextSource
import ru.workinprogress.telek.telegram.effect.telegramEffectExecutor

// Suppose your flow uses states of type YourState : State
@Serializable
data class YourState(
    val step: Int = 0,
) : State

fun persistenceModuleSample(contextSource: TelegramContextSource) {
    val userStateStore =
        PersistableUserStateStoreImpl<YourState>(
            stateStorageOf(dir = "./state".toPath()), // files like ./state/<chatId>.json
        )

    Telek(
        userStateStore = userStateStore,
        dispatchers = listOf(ExampleDispatcher()),
        effectExecutor = telegramEffectExecutor(contextSource),
    )
}
