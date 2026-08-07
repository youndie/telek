package ru.workinprogress.telek

import kotlin.reflect.KClass

class EffectRegistry {
    private val handlers = mutableMapOf<KClass<out Effect>, EffectHandler<out Effect>>()
    private val asyncHandlers = mutableMapOf<KClass<out Effect>, AsyncEffectHandler<out Effect>>()

    fun <E : Effect> register(
        effectClass: KClass<E>,
        handler: EffectHandler<E>,
    ) {
        handlers[effectClass] = handler
    }

    /** See [AsyncEffectHandler] — register an effect class here, not with [register], not both. */
    fun <E : Effect> registerAsync(
        effectClass: KClass<E>,
        handler: AsyncEffectHandler<E>,
    ) {
        asyncHandlers[effectClass] = handler
    }

    fun get(effectClass: KClass<out Effect>): EffectHandler<*>? = handlers[effectClass]

    fun getAsync(effectClass: KClass<out Effect>): AsyncEffectHandler<*>? = asyncHandlers[effectClass]
}
