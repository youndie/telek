package ru.workinprogress.telek

import java.util.logging.Level
import java.util.logging.Logger

interface EffectExecutor {
    suspend fun execute(
        context: ExecutionContext,
        effects: List<Effect>,
    )
}

class EffectExecutorImpl(
    private val effectRegistry: EffectRegistry,
) : EffectExecutor {
    private val logger = Logger.getLogger("EffectRegistry")

    override suspend fun execute(
        context: ExecutionContext,
        effects: List<Effect>,
    ) {
        for (effect in effects) {
            runCatching {
                @Suppress("UNCHECKED_CAST")
                (effectRegistry.get(effect::class) as? EffectHandler<Effect>)?.handle(context, effect)
            }.onFailure { e ->
                logger.log(Level.SEVERE, "Effect ${effect::class.simpleName} failed: ${e.message}", e)
            }
        }
    }
}
