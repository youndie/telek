package ru.workinprogress.telek.ktg.effect

import ru.workinprogress.telek.EffectRegistry
import ru.workinprogress.telek.ktg.effect.handler.EditMarkupEffectHandler
import ru.workinprogress.telek.ktg.effect.handler.EditMessageEffectHandler
import ru.workinprogress.telek.ktg.effect.handler.SendMessageEffectHandler

fun defaultEffectRegistry() =
    EffectRegistry().apply {
        register(SendMessageEffect::class, SendMessageEffectHandler())
        register(EditMessageEffect::class, EditMessageEffectHandler())
        register(EditMarkupEffect::class, EditMarkupEffectHandler())
    }
