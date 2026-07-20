package ru.workinprogress.telek

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface EffectExecutor {
    suspend fun execute(effects: List<Effect>): List<EffectResult>
}

/** What to do when an effect in a batch fails. */
enum class EffectFailurePolicy {
    /** Keep running the remaining effects in the batch (matches telek's historical behavior). */
    CONTINUE,

    /** Stop at the first failure; the returned list is shorter than [Effect]s given to [EffectExecutorImpl.execute]. */
    FAIL_FAST,
}

/**
 * Looks up a handler per effect in [effectRegistry] and runs it. [context] is resolved once per
 * [execute] call — transport-specific executors (e.g. `telegramEffectExecutor()`) use this to
 * supply an [ExecutionContext] that may only become available after telek itself is constructed
 * (e.g. once a bot instance exists), without [Telek] needing to own or track that lifecycle.
 *
 * Handlers run on [Dispatchers.IO], since telek ships handlers that do blocking I/O
 * (kotlin-telegram-bot's client) — this keeps them off whatever dispatcher [Telek]'s
 * per-chat workers run on.
 */
class EffectExecutorImpl(
    private val effectRegistry: EffectRegistry,
    private val context: suspend () -> ExecutionContext,
    private val failurePolicy: EffectFailurePolicy = EffectFailurePolicy.CONTINUE,
    private val logger: TelekLogger = TelekLogger.NoOp,
) : EffectExecutor {
    override suspend fun execute(effects: List<Effect>): List<EffectResult> {
        if (effects.isEmpty()) return emptyList()
        val resolvedContext = context()

        val results = mutableListOf<EffectResult>()
        for (effect in effects) {
            val result = executeOne(resolvedContext, effect)
            results += result
            if (failurePolicy == EffectFailurePolicy.FAIL_FAST && result is EffectFailed) break
        }
        return results
    }

    private suspend fun executeOne(
        context: ExecutionContext,
        effect: Effect,
    ): EffectResult =
        withContext(Dispatchers.IO) {
            runCatching {
                @Suppress("UNCHECKED_CAST")
                (effectRegistry.get(effect::class) as? EffectHandler<Effect>)?.handle(context, effect)
            }.fold({
                it ?: EffectFailed(IllegalStateException("EffectHandler not found for ${effect::class.simpleName}"))
            }, { e ->
                logger.error("Effect ${effect::class.simpleName} failed: ${e.message}", e)
                EffectFailed(e)
            })
        }
}
