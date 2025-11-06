package ru.workinprogress.telek

import kotlin.reflect.KClass

class EffectRegistry {
    private val handlers = mutableMapOf<KClass<out Effect>, EffectHandler<out Effect>>()

    fun <E : Effect> register(
        effectClass: KClass<E>,
        handler: EffectHandler<E>,
    ) {
        handlers[effectClass] = handler
    }

    fun get(effectClass: KClass<out Effect>): EffectHandler<*>? = handlers[effectClass]
}
