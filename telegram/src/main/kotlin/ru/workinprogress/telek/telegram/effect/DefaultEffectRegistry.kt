package ru.workinprogress.telek.telegram.effect

import ru.workinprogress.telek.EffectRegistry
import ru.workinprogress.telek.telegram.effect.handler.EditMarkupEffectHandler
import ru.workinprogress.telek.telegram.effect.handler.EditMessageEffectHandler
import ru.workinprogress.telek.telegram.effect.handler.SendMessageEffectHandler

fun defaultEffectRegistry() =
    EffectRegistry().apply {
        register(SendMessageEffect::class, SendMessageEffectHandler())
        register(EditMessageEffect::class, EditMessageEffectHandler())
        register(EditMarkupEffect::class, EditMarkupEffectHandler())
    }
