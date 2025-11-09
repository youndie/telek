package ru.workinprogress.telek

import java.util.logging.Level
import java.util.logging.Logger

interface EffectExecutor {
    suspend fun execute(
        context: ExecutionContext,
        effects: List<Effect>,
    ): List<EffectResult>
}

class EffectExecutorImpl(
    private val effectRegistry: EffectRegistry,
) : EffectExecutor {
    private val logger = Logger.getLogger("EffectRegistry")

    override suspend fun execute(
        context: ExecutionContext,
        effects: List<Effect>,
    ): List<EffectResult> =
        effects.map { effect ->
            runCatching {
                @Suppress("UNCHECKED_CAST")
                (effectRegistry.get(effect::class) as? EffectHandler<Effect>)?.handle(context, effect)
            }.fold({
                it ?: EffectFailed(IllegalStateException("EffectHandler not found for ${effect::class.simpleName}"))
            }, { e ->
                logger.log(Level.SEVERE, "Effect ${effect::class.simpleName} failed: ${e.message}", e)
                EffectFailed(e)
            })
        }
}
