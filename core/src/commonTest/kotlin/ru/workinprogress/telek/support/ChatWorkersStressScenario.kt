package ru.workinprogress.telek.support

import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import ru.workinprogress.telek.ChatWorkers
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * The one `ChatWorkers` scenario that needs *real* threads rather than `runTest`'s virtual
 * scheduler: it's the empirical repro from BACKLOG.md item 12, and it's what caught both original
 * race conditions. Since the multiplatform migration it also covers the atomicfu
 * [SynchronizedObject] that replaced `ConcurrentHashMap` — which is exactly the part a
 * single-platform run wouldn't prove.
 *
 * The body lives here, in `commonTest`, so JVM and Native run the identical scenario; only the
 * blocking entry point differs, because `runBlocking` is in kotlinx-coroutines' `concurrent`
 * source set rather than `common`. See `ChatWorkersStressTest` in `jvmTest`/`nativeTest`.
 */
internal suspend fun chatWorkersStressScenario() {
    val scope = CoroutineScope(Dispatchers.Default + Job())
    val workers = ChatWorkers(scope, idleTimeout = 1.milliseconds, inboxCapacity = 1000)
    val perChatSubmissions = 300
    val chatIds = (1L..6L).toList()
    val results = StressResults()

    coroutineScope {
        chatIds.forEach { chatId ->
            launch {
                repeat(perChatSubmissions) { i ->
                    workers.submit(chatId) { results.record(chatId, i) }
                    // Occasionally idle past the 1ms timeout, racing the next submit against that
                    // chat's worker retiring itself.
                    if (i % 3 == 0) delay(1)
                }
            }
        }
    }
    withTimeout(5.seconds) {
        while (results.total() < chatIds.size * perChatSubmissions) delay(5)
    }
    scope.cancel()

    chatIds.forEach { chatId ->
        assertEquals(
            (0 until perChatSubmissions).toList(),
            results.of(chatId),
            "chat $chatId lost or reordered work",
        )
    }
}

/** `ConcurrentHashMap` + `Collections.synchronizedList`, minus the JVM. */
private class StressResults {
    private val lock = SynchronizedObject()
    private val perChat = mutableMapOf<Long, MutableList<Int>>()

    fun record(
        chatId: Long,
        value: Int,
    ) = synchronized(lock) { perChat.getOrPut(chatId) { mutableListOf() }.add(value) }

    fun total(): Int = synchronized(lock) { perChat.values.sumOf { it.size } }

    fun of(chatId: Long): List<Int> = synchronized(lock) { perChat[chatId].orEmpty().toList() }
}
