package ru.workinprogress.telek

import kotlinx.coroutines.withContext

interface EffectExecutor {
    /**
     * Runs [effects] and returns one [EffectResult] per *synchronous* effect, in order. An effect
     * registered as async (see [EffectRegistry.registerAsync]) produces no [EffectResult] here —
     * instead, [dispatchAsync] is called with the effect's [Debounced.debounceKey] (or `null` if
     * it isn't [Debounced]) and a suspend block that runs that handler and returns its [Event];
     * the caller (telek's `Telek`) is responsible for actually launching that block somewhere
     * that outlives this [execute] call, for cancelling a previous same-key launch first, and for
     * routing a non-null [Event] back into the FSM. [dispatchAsync] is expected to be
     * fire-and-forget from this method's perspective.
     */
    suspend fun execute(
        effects: List<Effect>,
        dispatchAsync: (key: Any?, work: suspend () -> Event?) -> Unit,
    ): List<EffectResult>
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
 * Synchronous handlers run on [telekIoDispatcher], since telek ships handlers that do blocking I/O
 * (kotlin-telegram-bot's client) — this keeps them off whatever dispatcher [Telek]'s per-chat
 * workers run on. Async handlers (see [AsyncEffectHandler]) are handed to the caller-supplied
 * `dispatchAsync` as-is; where they actually run is up to the caller.
 */
class EffectExecutorImpl(
    private val effectRegistry: EffectRegistry,
    private val context: suspend () -> ExecutionContext,
    private val failurePolicy: EffectFailurePolicy = EffectFailurePolicy.CONTINUE,
    private val logger: TelekLogger = TelekLogger.NoOp,
) : EffectExecutor {
    override suspend fun execute(
        effects: List<Effect>,
        dispatchAsync: (key: Any?, work: suspend () -> Event?) -> Unit,
    ): List<EffectResult> {
        if (effects.isEmpty()) return emptyList()
        val resolvedContext = context()

        val results = mutableListOf<EffectResult>()
        for (effect in effects) {
            @Suppress("UNCHECKED_CAST")
            val asyncHandler = effectRegistry.getAsync(effect::class) as? AsyncEffectHandler<Effect>
            if (asyncHandler != null) {
                val key = (effect as? Debounced)?.debounceKey
                dispatchAsync(key) { runAsync(resolvedContext, effect, asyncHandler) }
                continue
            }

            val result = executeOne(resolvedContext, effect)
            results += result
            if (failurePolicy == EffectFailurePolicy.FAIL_FAST && result is EffectFailed) break
        }
        return results
    }

    private suspend fun runAsync(
        context: ExecutionContext,
        effect: Effect,
        handler: AsyncEffectHandler<Effect>,
    ): Event? =
        runCatching { handler.handle(context, effect) }
            .onFailure { logger.error("Async effect ${effect::class.simpleName} failed: ${it.message}", it) }
            .getOrNull()

    private suspend fun executeOne(
        context: ExecutionContext,
        effect: Effect,
    ): EffectResult =
        withContext(telekIoDispatcher) {
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
