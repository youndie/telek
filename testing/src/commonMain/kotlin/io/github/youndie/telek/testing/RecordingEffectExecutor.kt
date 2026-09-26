package io.github.youndie.telek.testing

import io.github.youndie.telek.Debounced
import io.github.youndie.telek.Effect
import io.github.youndie.telek.EffectExecutor
import io.github.youndie.telek.EffectOutcome
import io.github.youndie.telek.EffectResult
import io.github.youndie.telek.EffectSuccess
import io.github.youndie.telek.Event
import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.concurrent.atomics.update

/**
 * An [EffectExecutor] that records every batch of effects it was asked to run instead of
 * actually executing them, and returns a configurable [EffectResult] per effect.
 *
 * Defaults every effect to [EffectSuccess]; pass [resultsFor] to script specific outcomes
 * (e.g. simulate a failed send) for dispatcher/`onEffectResult` tests.
 *
 * By default every effect is treated as synchronous. To simulate an async effect (see
 * `AsyncEffectHandler`), have [asyncWorkFor] return the suspend block that would have produced its
 * [Event] instead of `null` for that effect — it's handed to `dispatchAsync` exactly as a real
 * `EffectExecutorImpl` would (along with the effect's [Debounced.debounceKey], if it has one), so
 * `Telek` routes the resulting event — and any debounce cancellation — back into the FSM for real.
 *
 * [executed] and [effects] are snapshots, safe to read from the test's thread
 * while a chat worker is still recording: that is exactly how a harness waits for a bot to go quiet.
 * A snapshot does not follow later recordings — read the property again rather than keeping the list.
 */
@OptIn(ExperimentalAtomicApi::class)
public class RecordingEffectExecutor(
    private val resultsFor: (Effect) -> EffectResult = { EffectSuccess },
    private val asyncWorkFor: (Effect) -> (suspend () -> Event?)? = { null },
) : EffectExecutor {
    // Written by whichever thread runs the chat worker, read by the test's own. A plain mutable
    // list here threw `ConcurrentModificationException` out of `effects` on both platforms. An
    // immutable list swapped whole rather than a lock: a reader polling in a loop never makes the
    // recording thread wait, and a test is the last place for a lock's fairness to matter.
    private val journal = AtomicReference(emptyList<List<Effect>>())

    public val executed: List<List<Effect>> get() = journal.load()

    public val effects: List<Effect> get() = executed.flatten()

    override suspend fun execute(
        effects: List<Effect>,
        dispatchAsync: (key: Any?, work: suspend () -> Event?) -> Unit,
    ): List<EffectOutcome> {
        val batch = effects.toList()
        journal.update { it + listOf(batch) }
        val results = mutableListOf<EffectOutcome>()
        for (effect in effects) {
            val asyncWork = asyncWorkFor(effect)
            if (asyncWork != null) {
                dispatchAsync((effect as? Debounced)?.debounceKey, asyncWork)
                continue
            }
            results += EffectOutcome(effect, resultsFor(effect))
        }
        return results
    }
}
