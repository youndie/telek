package io.github.youndie.telek

import io.github.youndie.telek.support.chatWorkersStressScenario
import kotlinx.coroutines.runBlocking
import kotlin.test.Test

/** JVM entry point for [chatWorkersStressScenario] — the scenario itself lives in `commonTest`. */
class ChatWorkersStressTest {
    @Test
    fun `submissions racing directly against idle retirement are never lost or reordered`() =
        runBlocking {
            chatWorkersStressScenario()
        }
}
