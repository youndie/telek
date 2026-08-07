package ru.workinprogress.telek.ktg.effect

import ru.workinprogress.telek.EffectExecutor
import ru.workinprogress.telek.EffectExecutorImpl
import ru.workinprogress.telek.EffectFailurePolicy
import ru.workinprogress.telek.EffectRegistry
import ru.workinprogress.telek.TelekLogger
import ru.workinprogress.telek.ktg.KtgContextSource

fun ktgEffectExecutor(
    contextSource: KtgContextSource,
    effectRegistry: EffectRegistry = defaultEffectRegistry(),
    failurePolicy: EffectFailurePolicy = EffectFailurePolicy.CONTINUE,
    logger: TelekLogger = TelekLogger.NoOp,
): EffectExecutor =
    EffectExecutorImpl(
        effectRegistry = effectRegistry,
        context = contextSource::context,
        failurePolicy = failurePolicy,
        logger = logger,
    )
