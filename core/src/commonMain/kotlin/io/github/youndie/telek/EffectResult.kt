package io.github.youndie.telek

public interface EffectResult

public object EffectSuccess : EffectResult

public class EffectFailed(
    public val error: Throwable,
) : EffectResult
