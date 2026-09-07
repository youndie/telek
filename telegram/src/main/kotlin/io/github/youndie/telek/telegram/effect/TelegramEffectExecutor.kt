package io.github.youndie.telek.telegram.effect

import io.github.youndie.telek.EffectExecutor
import io.github.youndie.telek.EffectExecutorImpl
import io.github.youndie.telek.EffectFailurePolicy
import io.github.youndie.telek.EffectRegistry
import io.github.youndie.telek.TelekLogger
import io.github.youndie.telek.telegram.TelegramContextSource

fun telegramEffectExecutor(
    contextSource: TelegramContextSource,
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
