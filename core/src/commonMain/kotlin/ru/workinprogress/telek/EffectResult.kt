package ru.workinprogress.telek

interface EffectResult

object EffectSuccess : EffectResult

class EffectFailed(
    val error: Throwable,
) : EffectResult
