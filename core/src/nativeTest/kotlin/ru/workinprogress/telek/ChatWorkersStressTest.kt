package ru.workinprogress.telek

import kotlinx.coroutines.runBlocking
import ru.workinprogress.telek.support.chatWorkersStressScenario
import kotlin.test.Test

/**
 * Native entry point for [chatWorkersStressScenario]. This is the run that actually exercises
 * atomicfu's POSIX-mutex `SynchronizedObject` under real threads; it is *skipped* on a macOS host
 * (a linuxX64 binary can't run there), so it only proves anything on a Linux runner.
 */
class ChatWorkersStressTest {
    @Test
    fun `submissions racing directly against idle retirement are never lost or reordered`() = runBlocking { chatWorkersStressScenario() }
}
