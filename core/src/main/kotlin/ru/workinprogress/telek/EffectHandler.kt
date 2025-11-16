package ru.workinprogress.telek

interface EffectHandler<E : Effect> {
    fun handle(
        context: ExecutionContext,
        effect: E,
    ): EffectResult
}
