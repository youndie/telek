package ru.workinprogress.telek

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.newFixedThreadPoolContext

/**
 * `Dispatchers.IO` is unreachable from a Kotlin/Native source set: kotlinx-coroutines declares the
 * public one as an `expect` extension in its `concurrent` source set, but `Dispatchers` on Native
 * also has an `internal val IO` member, and a member always shadows an extension — so the
 * reference resolves to the internal one and fails to compile outside kotlinx-coroutines itself.
 *
 * This mirrors what coroutines' own native `DefaultIoScheduler` does (a fixed pool sized like the
 * JVM's default IO parallelism), built lazily so a bot that never runs a blocking effect handler
 * never pays for the threads. Deliberately never closed — its lifetime is the process's.
 */
@OptIn(DelicateCoroutinesApi::class)
actual val telekIoDispatcher: CoroutineDispatcher by lazy {
    newFixedThreadPoolContext(nThreads = 64, name = "telek-io")
}
