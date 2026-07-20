@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package ru.workinprogress.telek

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.onTimeout
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.yield
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration

/**
 * Runs the work submitted for a given chatId strictly sequentially, one task at a time, in
 * submission order — regardless of how many callers concurrently call [submit] for that chatId.
 * Work submitted for different chatIds runs fully in parallel. Also lets a chat's async effects
 * (see [launchAsync]) run independently of that ordering, while still being tied to the chat's
 * lifecycle so they're cancelled when the chat goes idle.
 *
 * This is what lets [Telek] guarantee in-order processing per chat without any locking in
 * [UserStateStore] implementations: at most one [update][UserStateStore.update] call for a given
 * chatId is ever in flight, so implementations do not need to serialize themselves.
 *
 * A worker for a chatId is torn down after [idleTimeout] of inactivity and transparently
 * recreated on the next [submit] for that chatId, so memory use is bounded by *currently active*
 * chats rather than by every chatId ever seen.
 *
 * The inbox is unbounded: [submit] never suspends waiting for room. There is currently no
 * backpressure: a chat that produces work faster than its worker can process it will grow that
 * chat's inbox without bound. That's an intentional, documented gap (see telek's BACKLOG.md
 * item 12), not an oversight — a bounded inbox needs a dropping/rate-limiting policy on top of it
 * to stay correct, not just a capacity number.
 */
internal class ChatWorkers(
    private val scope: CoroutineScope,
    private val idleTimeout: Duration,
) {
    private val workers = ConcurrentHashMap<Long, Worker>()

    /** Enqueues [task] for [chatId] and returns once it has been accepted — not once it has run. */
    suspend fun submit(
        chatId: Long,
        task: suspend () -> Unit,
    ) {
        while (true) {
            val worker = workers.computeIfAbsent(chatId) { Worker(chatId) }
            if (worker.trySend(task)) return
            // This worker has already decided to retire but may still be draining its own
            // backlog; it removes itself from the map only once that's fully done (see retire()).
            // Retrying here — instead of removing it ourselves — is what keeps a replacement
            // worker from ever running concurrently with the old one's still-in-flight tail,
            // which would break per-chat ordering.
            yield()
        }
    }

    /**
     * Launches [task] independently of [submit]'s ordering — for async effects, which must not
     * block the transition that produced them. Fire-and-forget: doesn't wait for [task], and
     * [task] is cancelled if this chat's worker retires (see [Worker.retire]).
     *
     * Must be called from within a task already running on that chat's worker (i.e. from inside a
     * [submit]ted task — which is exactly where [Telek] calls it from, while executing a chat's
     * effects), so the worker is guaranteed to still be live and not mid-retirement.
     */
    fun launchAsync(
        chatId: Long,
        task: suspend () -> Unit,
    ) {
        workers.computeIfAbsent(chatId) { Worker(chatId) }.launchAsync(task)
    }

    private inner class Worker(
        private val chatId: Long,
    ) {
        // A dedicated scope per worker (not just `scope.launch` directly) is what lets async
        // effects launched via [launchAsync] be cancelled as a group when this worker retires,
        // without touching unrelated chats. SupervisorJob so one failing async effect doesn't
        // cancel the receive loop or any other in-flight async effect for this same chat.
        private val workerScope = CoroutineScope(scope.coroutineContext + SupervisorJob())
        private val inbox = Channel<suspend () -> Unit>(Channel.UNLIMITED)

        // Guards the transition from "accepting work" to "retired": every enqueue and the
        // retirement decision run inside this lock, so a submitter can never enqueue into a
        // channel this worker has already committed to draining and abandoning. A coroutine
        // Mutex, not `synchronized` — the critical section never suspends, but a raw JVM monitor
        // would still block the underlying thread while held, which is the kind of thing that
        // starves other coroutines on a dispatcher with limited parallelism. Mutex.withLock
        // suspends instead, so it composes correctly with the rest of this (all-coroutine) file.
        private val lifecycle = Mutex()
        private var retired = false

        /** Returns false if this worker has already retired and won't process anything more. */
        suspend fun trySend(task: suspend () -> Unit): Boolean =
            lifecycle.withLock {
                if (retired) {
                    false
                } else {
                    inbox.trySend(task).isSuccess
                }
            }

        fun launchAsync(task: suspend () -> Unit) {
            workerScope.launch { task() }
        }

        init {
            workerScope.launch {
                while (true) {
                    // Deliberately not `withTimeoutOrNull(idleTimeout) { inbox.receive() }`: that
                    // pattern can drop an element that becomes available at the same instant the
                    // timeout fires — `receive()`'s own cancellation safety doesn't cover being
                    // raced by an enclosing `withTimeout`. `select` + `onTimeout` is the
                    // documented, race-free way to express "receive or time out".
                    val task =
                        select<(suspend () -> Unit)?> {
                            inbox.onReceive { it }
                            onTimeout(idleTimeout) { null }
                        }
                    if (task == null) {
                        if (retire()) return@launch
                        // Lost the (in practice unreachable, but not provably impossible) race
                        // of retiring concurrently with ourselves — keep serving.
                        continue
                    }
                    task()
                }
            }
        }

        /**
         * Called after an idle timeout. Flips [retired] under [lifecycle] first — so no further
         * [trySend] call can succeed — then drains everything already buffered (which, since no
         * more sends can land, is now the complete and final backlog) in submission order, and
         * only *after* that's fully done removes this worker from the map. Removing from the map
         * only at the end, rather than up front, is what stops a replacement worker from being
         * created and running concurrently with this one's tail end — see [submit]. Finally
         * cancels [workerScope], stopping any still-in-flight async effects for this chat.
         * Returns whether this worker actually retired.
         */
        private suspend fun retire(): Boolean {
            val won =
                lifecycle.withLock {
                    if (retired) {
                        false
                    } else {
                        retired = true
                        true
                    }
                }
            if (!won) return false

            while (true) {
                val leftover = inbox.tryReceive().getOrNull() ?: break
                leftover()
            }
            workers.remove(chatId, this)
            workerScope.cancel()
            return true
        }
    }
}
