package io.github.youndie.telek

public interface EffectHandler<E : Effect> {
    public suspend fun handle(
        context: ExecutionContext,
        effect: E,
    ): EffectResult
}
