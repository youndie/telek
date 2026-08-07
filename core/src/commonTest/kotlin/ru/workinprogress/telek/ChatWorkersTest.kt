@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package ru.workinprogress.telek

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes

class ChatWorkersTest {
    @Test
    fun `tasks for the same chatId run strictly in submission order`() =
        runTest {
            val workers = ChatWorkers(scope = this, idleTimeout = 15.minutes, inboxCapacity = 1000)
            val order = mutableListOf<Int>()

            (1..50).forEach { i -> launch { workers.submit(chatId = 1) { order += i } } }
            advanceUntilIdle()

            assertEquals((1..50).toList(), order)
        }

    @Test
    fun `different chatIds are not serialized against each other`() =
        runTest {
            val workers = ChatWorkers(scope = this, idleTimeout = 15.minutes, inboxCapacity = 1000)
            var chat2Completed = false

            launch { workers.submit(chatId = 1) { delay(10.minutes) } }
            launch { workers.submit(chatId = 2) { chat2Completed = true } }

            // Only runs work already ready at the current instant of virtual time — chat 1's task
            // is parked on `delay`, not ready, so this proves chat 2 didn't queue up behind it.
            runCurrent()

            assertTrue(chat2Completed)
        }

    @Test
    fun `a worker retires after idleTimeout and a later submit transparently creates a fresh one`() =
        runTest {
            val workers = ChatWorkers(scope = this, idleTimeout = 100.milliseconds, inboxCapacity = 1000)
            var firstRan = false
            var secondRan = false

            workers.submit(chatId = 1) { firstRan = true }
            runCurrent()
            assertTrue(firstRan)

            advanceTimeBy(200.milliseconds) // idle past the timeout — the worker retires itself
            runCurrent()

            workers.submit(chatId = 1) { secondRan = true }
            runCurrent()

            assertTrue(secondRan)
        }

    @Test
    fun `launchAsync work does not block subsequent submitted tasks for the same chat`() =
        runTest {
            val workers = ChatWorkers(scope = this, idleTimeout = 15.minutes, inboxCapacity = 1000)
            var asyncCompleted = false
            var nextTaskRan = false

            workers.submit(chatId = 1) {
                workers.launchAsync(chatId = 1) {
                    delay(10.minutes)
                    asyncCompleted = true
                }
            }
            runCurrent()

            workers.submit(chatId = 1) { nextTaskRan = true }
            runCurrent()

            assertTrue(nextTaskRan)
            assertFalse(asyncCompleted) // still pending — we never advanced 10 minutes
        }

    @Test
    fun `async work is cancelled when its chat's worker retires`() =
        runTest {
            val workers = ChatWorkers(scope = this, idleTimeout = 100.milliseconds, inboxCapacity = 1000)
            var completed = false
            var cancelled = false

            workers.submit(chatId = 1) {
                workers.launchAsync(chatId = 1) {
                    try {
                        delay(10.minutes)
                        completed = true
                    } catch (e: CancellationException) {
                        cancelled = true
                        throw e
                    }
                }
            }
            runCurrent()

            advanceTimeBy(200.milliseconds) // idle past the timeout — worker retires, cancelling it
            runCurrent()

            assertTrue(cancelled)
            assertFalse(completed)
        }

    @Test
    fun `submit drops new work and logs a warning once the chat's inbox is full`() =
        runTest {
            val warnings = mutableListOf<String>()
            val logger =
                object : TelekLogger {
                    override fun log(
                        level: TelekLogLevel,
                        message: String,
                        error: Throwable?,
                    ) {
                        if (level == TelekLogLevel.WARN) warnings += message
                    }
                }
            val workers = ChatWorkers(scope = this, idleTimeout = 15.minutes, inboxCapacity = 2, logger = logger)
            val ran = mutableListOf<Int>()

            // Block the worker on a long-running first task so subsequent submits pile up in the
            // inbox instead of being drained immediately.
            launch {
                workers.submit(chatId = 1) {
                    delay(10.minutes)
                    ran += 0
                }
            }
            runCurrent() // worker picks up task 0 and is now suspended inside it

            launch { workers.submit(chatId = 1) { ran += 1 } }
            launch { workers.submit(chatId = 1) { ran += 2 } } // inbox now full (capacity = 2)
            runCurrent()
            launch { workers.submit(chatId = 1) { ran += 3 } } // dropped — inbox has no room
            runCurrent()

            assertTrue(warnings.any { it.contains("chatId=1") })

            advanceTimeBy(11.minutes)
            runCurrent()

            // Task 3 never ran — it was dropped outright, not merely delayed.
            assertEquals(listOf(0, 1, 2), ran)
        }

    @Test
    fun `launchAsync with a key cancels the previous in-flight job for the same key`() =
        runTest {
            val workers = ChatWorkers(scope = this, idleTimeout = 15.minutes, inboxCapacity = 1000)
            var firstCancelled = false
            var secondCompleted = false

            workers.submit(chatId = 1) {
                workers.launchAsync(chatId = 1, key = "search") {
                    try {
                        delay(10.minutes)
                    } catch (e: CancellationException) {
                        firstCancelled = true
                        throw e
                    }
                }
            }
            runCurrent() // let the first job actually start and suspend on delay before cancelling it

            workers.submit(chatId = 1) {
                workers.launchAsync(chatId = 1, key = "search") {
                    secondCompleted = true
                }
            }
            runCurrent()

            assertTrue(firstCancelled)
            assertTrue(secondCompleted)
        }

    @Test
    fun `launchAsync jobs with different keys run independently — neither cancels the other`() =
        runTest {
            val workers = ChatWorkers(scope = this, idleTimeout = 15.minutes, inboxCapacity = 1000)
            var firstCompleted = false
            var secondCompleted = false

            workers.submit(chatId = 1) { workers.launchAsync(chatId = 1, key = "a") { firstCompleted = true } }
            workers.submit(chatId = 1) { workers.launchAsync(chatId = 1, key = "b") { secondCompleted = true } }
            runCurrent()

            assertTrue(firstCompleted)
            assertTrue(secondCompleted)
        }

    @Test
    fun `launchAsync without a key never auto-cancels a previous launch`() =
        runTest {
            val workers = ChatWorkers(scope = this, idleTimeout = 15.minutes, inboxCapacity = 1000)
            var firstCompleted = false
            var secondCompleted = false

            workers.submit(chatId = 1) {
                workers.launchAsync(chatId = 1) {
                    delay(1.milliseconds)
                    firstCompleted = true
                }
            }
            workers.submit(chatId = 1) { workers.launchAsync(chatId = 1) { secondCompleted = true } }
            runCurrent()
            advanceTimeBy(2.milliseconds)
            runCurrent()

            assertTrue(firstCompleted)
            assertTrue(secondCompleted)
        }
}
