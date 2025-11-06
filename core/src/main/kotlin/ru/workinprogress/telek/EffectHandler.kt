package ru.workinprogress.telek

interface EffectHandler<E : Effect> {
    suspend fun handle(
        context: ExecutionContext,
        effect: E,
    )
}
