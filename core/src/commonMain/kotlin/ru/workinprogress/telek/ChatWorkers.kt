@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package ru.workinprogress.telek

import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.onTimeout
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.yield
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
 * [submit] never suspends waiting for room: each chat's inbox holds at most [inboxCapacity]
 * pending tasks. Once full, further [submit] calls for that chat **drop** the new task and log a
 * warning via [logger] rather than growing without bound or blocking the caller — a chat that's
 * producing input faster than it can be processed (e.g. a user hammering an inline button) loses
 * the newest input instead of exhausting memory or stalling whoever calls [submit].
 */
internal class ChatWorkers(
    private val scope: CoroutineScope,
    private val idleTimeout: Duration,
    private val inboxCapacity: Int,
    private val logger: TelekLogger = TelekLogger.NoOp,
) {
    // Replaces the JVM-only ConcurrentHashMap this used to be: `SynchronizedObject` is atomicfu's
    // multiplatform monitor (a real monitor on JVM, a POSIX mutex on Native). Every critical
    // section here is a single map lookup/insert/removal that never suspends — see [workerFor] for
    // why worker *construction* is deliberately kept outside the lock.
    private val workersLock = SynchronizedObject()
    private val workers = mutableMapOf<Long, Worker>()

    /**
     * `computeIfAbsent`'s replacement. A [Worker] must not be built while [workersLock] is held:
     * starting its receive loop launches a coroutine, and a coroutine that suspends inside a
     * platform monitor holds that monitor while suspended. So the worker is constructed outside
     * the lock, published under it, and only started if it actually won the insert — a loser is
     * simply dropped, never having started anything.
     */
    private fun workerFor(chatId: Long): Worker {
        synchronized(workersLock) { workers[chatId] }?.let { return it }

        val candidate = Worker(chatId)
        val winner = synchronized(workersLock) { workers.getOrPut(chatId) { candidate } }
        if (winner === candidate) candidate.start()
        return winner
    }

    /**
     * Enqueues [task] for [chatId] and returns once it has either been accepted or dropped — not
     * once it has run. See the class doc for what happens when that chat's inbox is full.
     */
    suspend fun submit(
        chatId: Long,
        task: suspend () -> Unit,
    ) {
        while (true) {
            val worker = workerFor(chatId)
            when (worker.trySend(task)) {
                SendOutcome.ACCEPTED -> {
                    return
                }

                SendOutcome.RETIRED -> {
                    // This worker has already decided to retire but may still be draining its own
                    // backlog; it removes itself from the map only once that's fully done (see
                    // retire()). Retrying here — instead of removing it ourselves — is what keeps
                    // a replacement worker from ever running concurrently with the old one's
                    // still-in-flight tail, which would break per-chat ordering.
                    yield()
                }

                SendOutcome.DROPPED -> {
                    logger.warn("Dropping input for chatId=$chatId — inbox is full (capacity=$inboxCapacity)")
                    return
                }
            }
        }
    }

    /**
     * Launches [task] independently of [submit]'s ordering — for async effects, which must not
     * block the transition that produced them. Fire-and-forget: doesn't wait for [task], and
     * [task] is cancelled if this chat's worker retires (see [Worker.retire]).
     *
     * If [key] is non-null (see [Debounced]), any previously launched, still-running [task] for
     * this same chat and [key] is cancelled first — at most one debounced job per (chatId, key)
     * runs at a time. `null` (the default) means "never auto-cancelled".
     *
     * Must be called from within a task already running on that chat's worker (i.e. from inside a
     * [submit]ted task — which is exactly where [Telek] calls it from, while executing a chat's
     * effects), so the worker is guaranteed to still be live and not mid-retirement.
     */
    fun launchAsync(
        chatId: Long,
        key: Any? = null,
        task: suspend () -> Unit,
    ) {
        workerFor(chatId).launchAsync(key, task)
    }

    private enum class SendOutcome { ACCEPTED, RETIRED, DROPPED }

    private inner class Worker(
        private val chatId: Long,
    ) {
        // A dedicated scope per worker (not just `scope.launch` directly) is what lets async
        // effects launched via [launchAsync] be cancelled as a group when this worker retires,
        // without touching unrelated chats. SupervisorJob so one failing async effect doesn't
        // cancel the receive loop or any other in-flight async effect for this same chat.
        private val workerScope = CoroutineScope(scope.coroutineContext + SupervisorJob())
        private val inbox = Channel<suspend () -> Unit>(inboxCapacity)

        // Guards the transition from "accepting work" to "retired": every enqueue and the
        // retirement decision run inside this lock, so a submitter can never enqueue into a
        // channel this worker has already committed to draining and abandoning. A coroutine
        // Mutex, not `synchronized` — the critical section never suspends, but a raw JVM monitor
        // would still block the underlying thread while held, which is the kind of thing that
        // starves other coroutines on a dispatcher with limited parallelism. Mutex.withLock
        // suspends instead, so it composes correctly with the rest of this (all-coroutine) file.
        private val lifecycle = Mutex()
        private var retired = false

        // launchAsync() itself only ever runs on this worker's own currently-executing task, but
        // the invokeOnCompletion callback it registers fires on whichever thread completed the
        // job — so this map genuinely is touched concurrently and needs a lock of its own, small
        // as the critical sections are. (`MutableMap.remove(key, value)` is JVM-only, hence the
        // explicit identity check when removing.)
        private val debouncedLock = SynchronizedObject()
        private val debouncedJobs = mutableMapOf<Any, Job>()

        /**
         * Uses the channel's non-suspending `trySend` rather than `send` — a full inbox should
         * drop the task and report back immediately, not suspend the caller waiting for room
         * (that would just turn "the chat is behind" into "the caller is blocked", not fix it).
         * This is also what makes it safe to call `trySend` while holding [lifecycle]: it never
         * suspends, so the lock is only ever held for plain, synchronous bookkeeping.
         */
        suspend fun trySend(task: suspend () -> Unit): SendOutcome =
            lifecycle.withLock {
                if (retired) {
                    SendOutcome.RETIRED
                } else if (inbox.trySend(task).isSuccess) {
                    SendOutcome.ACCEPTED
                } else {
                    SendOutcome.DROPPED
                }
            }

        fun launchAsync(
            key: Any?,
            task: suspend () -> Unit,
        ) {
            if (key != null) {
                // Read under the lock, cancel outside it: cancellation can synchronously run the
                // previous job's own completion handler, which takes this same lock.
                synchronized(debouncedLock) { debouncedJobs[key] }?.cancel()
            }
            val job = workerScope.launch { task() }
            if (key != null) {
                synchronized(debouncedLock) { debouncedJobs[key] = job }
                job.invokeOnCompletion {
                    synchronized(debouncedLock) {
                        if (debouncedJobs[key] === job) debouncedJobs.remove(key)
                    }
                }
            }
        }

        /**
         * Starts the receive loop. Separate from construction (rather than an `init` block) so
         * [workerFor] can build a worker outside [workersLock] and only spin it up once it has
         * won the race to be published — see there.
         */
        fun start() {
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
            // Remove-if-still-us: a replacement worker may already have taken this chatId's slot.
            synchronized(workersLock) {
                if (workers[chatId] === this) workers.remove(chatId)
            }
            workerScope.cancel()
            return true
        }
    }
}
