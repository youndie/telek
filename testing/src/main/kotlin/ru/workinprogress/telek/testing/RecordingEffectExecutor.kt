package ru.workinprogress.telek.testing

import ru.workinprogress.telek.Effect
import ru.workinprogress.telek.EffectExecutor
import ru.workinprogress.telek.EffectResult
import ru.workinprogress.telek.EffectSuccess
import ru.workinprogress.telek.Event

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
 * `EffectExecutorImpl` would, so `Telek` routes the resulting event back into the FSM for real.
 */
class RecordingEffectExecutor(
    private val resultsFor: (Effect) -> EffectResult = { EffectSuccess },
    private val asyncWorkFor: (Effect) -> (suspend () -> Event?)? = { null },
) : EffectExecutor {
    private val _executed = mutableListOf<List<Effect>>()
    val executed: List<List<Effect>> get() = _executed

    val effects: List<Effect> get() = _executed.flatten()

    override suspend fun execute(
        effects: List<Effect>,
        dispatchAsync: (suspend () -> Event?) -> Unit,
    ): List<EffectResult> {
        _executed += effects
        val results = mutableListOf<EffectResult>()
        for (effect in effects) {
            val asyncWork = asyncWorkFor(effect)
            if (asyncWork != null) {
                dispatchAsync(asyncWork)
                continue
            }
            results += resultsFor(effect)
        }
        return results
    }
}
