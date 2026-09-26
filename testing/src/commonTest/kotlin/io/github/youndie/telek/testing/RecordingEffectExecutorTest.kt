package io.github.youndie.telek.testing

import io.github.youndie.telek.Effect
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlin.test.Test
import kotlin.test.assertEquals

class RecordingEffectExecutorTest {
    private object Ping : Effect

    // Telek runs effects on a chat worker, while a test reads the journal from its own thread to
    // decide whether the bot has gone quiet. Found as a `ConcurrentModificationException` out of
    // `effects` in a consumer's 500-cycle run on linuxX64; the menu scenarios never hit it, because
    // they read the journal a handful of times.
    @Test
    fun readingTheJournalWhileEffectsAreRecordedDoesNotThrow() =
        runTest {
            val executor = RecordingEffectExecutor()
            val writer =
                launch(Dispatchers.Default) {
                    repeat(WRITES) { executor.execute(listOf(Ping, Ping)) { _, _ -> } }
                }
            withContext(Dispatchers.Default) {
                while (writer.isActive) {
                    executor.effects.size
                    executor.executed.size
                }
            }
            writer.join()

            assertEquals(WRITES, executor.executed.size)
            assertEquals(WRITES * 2, executor.effects.size)
        }

    private companion object {
        const val WRITES = 2_000
    }
}
