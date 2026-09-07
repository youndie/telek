package io.github.youndie.telek.ktg.effect

import io.github.youndie.telek.EffectRegistry
import io.github.youndie.telek.ktg.effect.handler.EditMarkupEffectHandler
import io.github.youndie.telek.ktg.effect.handler.EditMessageEffectHandler
import io.github.youndie.telek.ktg.effect.handler.SendMessageEffectHandler

fun defaultEffectRegistry() =
    EffectRegistry().apply {
        register(SendMessageEffect::class, SendMessageEffectHandler())
        register(EditMessageEffect::class, EditMessageEffectHandler())
        register(EditMarkupEffect::class, EditMarkupEffectHandler())
    }
