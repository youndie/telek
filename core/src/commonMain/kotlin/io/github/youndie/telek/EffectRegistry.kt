package io.github.youndie.telek

import kotlin.reflect.KClass

public class EffectRegistry {
    private val handlers = mutableMapOf<KClass<out Effect>, EffectHandler<out Effect>>()
    private val asyncHandlers = mutableMapOf<KClass<out Effect>, AsyncEffectHandler<out Effect>>()

    public fun <E : Effect> register(
        effectClass: KClass<E>,
        handler: EffectHandler<E>,
    ) {
        handlers[effectClass] = handler
    }

    /** See [AsyncEffectHandler] — register an effect class here, not with [register], not both. */
    public fun <E : Effect> registerAsync(
        effectClass: KClass<E>,
        handler: AsyncEffectHandler<E>,
    ) {
        asyncHandlers[effectClass] = handler
    }

    public fun get(effectClass: KClass<out Effect>): EffectHandler<*>? = handlers[effectClass]

    public fun getAsync(effectClass: KClass<out Effect>): AsyncEffectHandler<*>? = asyncHandlers[effectClass]
}
