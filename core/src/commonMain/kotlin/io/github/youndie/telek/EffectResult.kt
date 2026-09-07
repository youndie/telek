package io.github.youndie.telek

interface EffectResult

object EffectSuccess : EffectResult

class EffectFailed(
    val error: Throwable,
) : EffectResult
