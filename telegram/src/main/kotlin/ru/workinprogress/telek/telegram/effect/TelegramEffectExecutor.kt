package ru.workinprogress.telek.telegram.effect

import ru.workinprogress.telek.EffectExecutor
import ru.workinprogress.telek.EffectExecutorImpl

fun telegramEffectExecutor(): EffectExecutor = EffectExecutorImpl(defaultEffectRegistry())
