@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package ru.workinprogress.telek

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class ChatWorkersTest {
    @Test
    fun `tasks for the same chatId run strictly in submission order`() =
        runTest {
            val workers = ChatWorkers(scope = this, idleTimeout = 15.minutes)
            val order = mutableListOf<Int>()

            (1..50).forEach { i -> launch { workers.submit(chatId = 1) { order += i } } }
            advanceUntilIdle()

            assertEquals((1..50).toList(), order)
        }

    @Test
    fun `different chatIds are not serialized against each other`() =
        runTest {
            val workers = ChatWorkers(scope = this, idleTimeout = 15.minutes)
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
            val workers = ChatWorkers(scope = this, idleTimeout = 100.milliseconds)
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
    fun `submissions racing directly against idle retirement are never lost or reordered`() =
        runBlocking {
            val scope = CoroutineScope(Dispatchers.Default + Job())
            val workers = ChatWorkers(scope, idleTimeout = 1.milliseconds)
            val perChatSubmissions = 300
            val chatIds = (1L..6L).toList()
            val results = ConcurrentHashMap<Long, MutableList<Int>>()

            val jobs =
                chatIds.map { chatId ->
                    launch {
                        repeat(perChatSubmissions) { i ->
                            workers.submit(chatId) {
                                results.getOrPut(chatId) { Collections.synchronizedList(mutableListOf<Int>()) }.add(i)
                            }
                            // Occasionally idle past the 1ms timeout, racing the next submit
                            // against that chat's worker retiring itself.
                            if (i % 3 == 0) delay(1)
                        }
                    }
                }
            jobs.forEach { it.join() }
            withTimeout(5.seconds) {
                while (results.values.sumOf { it.size } < chatIds.size * perChatSubmissions) delay(5)
            }
            scope.cancel()

            chatIds.forEach { chatId ->
                assertEquals(
                    (0 until perChatSubmissions).toList(),
                    results[chatId].orEmpty().toList(),
                    "chat $chatId lost or reordered work",
                )
            }
        }
}
