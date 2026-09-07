// Compiled copy of README.md's "Persistence module" section — keep both in sync.
package io.github.youndie.telek.docs

import io.github.youndie.telek.State
import io.github.youndie.telek.Telek
import io.github.youndie.telek.persistence.PersistableUserStateStoreImpl
import io.github.youndie.telek.persistence.stateStorageOf
import io.github.youndie.telek.telegram.TelegramContextSource
import io.github.youndie.telek.telegram.effect.telegramEffectExecutor
import kotlinx.serialization.Serializable
import okio.Path.Companion.toPath

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
