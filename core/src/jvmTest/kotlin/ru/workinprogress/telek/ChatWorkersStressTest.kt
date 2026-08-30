package ru.workinprogress.telek

import kotlinx.coroutines.runBlocking
import ru.workinprogress.telek.support.chatWorkersStressScenario
import kotlin.test.Test

/** JVM entry point for [chatWorkersStressScenario] — the scenario itself lives in `commonTest`. */
class ChatWorkersStressTest {
    @Test
    fun `submissions racing directly against idle retirement are never lost or reordered`() =
        runBlocking {
            chatWorkersStressScenario()
        }
}
