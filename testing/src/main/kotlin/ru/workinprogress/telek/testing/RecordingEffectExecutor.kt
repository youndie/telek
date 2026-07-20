package ru.workinprogress.telek.testing

import ru.workinprogress.telek.Effect
import ru.workinprogress.telek.EffectExecutor
import ru.workinprogress.telek.EffectResult
import ru.workinprogress.telek.EffectSuccess
import ru.workinprogress.telek.ExecutionContext

/**
 * An [EffectExecutor] that records every batch of effects it was asked to run instead of
 * actually executing them, and returns a configurable [EffectResult] per effect.
 *
 * Defaults every effect to [EffectSuccess]; pass [resultsFor] to script specific outcomes
 * (e.g. simulate a failed send) for dispatcher/`onEffectResult` tests.
 */
class RecordingEffectExecutor(
    private val resultsFor: (Effect) -> EffectResult = { EffectSuccess },
) : EffectExecutor {
    private val _executed = mutableListOf<Pair<ExecutionContext, List<Effect>>>()
    val executed: List<Pair<ExecutionContext, List<Effect>>> get() = _executed

    val effects: List<Effect> get() = _executed.flatMap { it.second }

    override fun execute(
        context: ExecutionContext,
        effects: List<Effect>,
    ): List<EffectResult> {
        _executed += context to effects
        return effects.map(resultsFor)
    }
}
