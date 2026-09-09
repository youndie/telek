package io.github.youndie.telek.telegram.effect

import io.github.youndie.telek.EffectRegistry
import io.github.youndie.telek.telegram.effect.handler.EditMarkupEffectHandler
import io.github.youndie.telek.telegram.effect.handler.EditMessageEffectHandler
import io.github.youndie.telek.telegram.effect.handler.SendMessageEffectHandler

public fun defaultEffectRegistry(): EffectRegistry =
    EffectRegistry().apply {
        register(SendMessageEffect::class, SendMessageEffectHandler())
        register(EditMessageEffect::class, EditMessageEffectHandler())
        register(EditMarkupEffect::class, EditMarkupEffectHandler())
    }
