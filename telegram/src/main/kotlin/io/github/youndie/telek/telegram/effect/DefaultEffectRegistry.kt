@file:Suppress("DEPRECATION")

package io.github.youndie.telek.telegram.effect

import io.github.youndie.telek.EffectRegistry
import io.github.youndie.telek.telegram.effect.handler.EditMarkdownMessageEffectHandler
import io.github.youndie.telek.telegram.effect.handler.EditMarkupEffectHandler
import io.github.youndie.telek.telegram.effect.handler.EditMessageEffectHandler
import io.github.youndie.telek.telegram.effect.handler.SendMarkdownMessageEffectHandler
import io.github.youndie.telek.telegram.effect.handler.SendMessageEffectHandler

public fun defaultEffectRegistry(): EffectRegistry =
    EffectRegistry().apply {
        register(SendMessageEffect::class, SendMessageEffectHandler())
        register(EditMessageEffect::class, EditMessageEffectHandler())
        register(EditMarkupEffect::class, EditMarkupEffectHandler())
        // The pre-entities path. Registered so a bot that has not migrated keeps working; both
        // lines go when the deprecated effects do.
        register(SendMarkdownMessageEffect::class, SendMarkdownMessageEffectHandler())
        register(EditMarkdownMessageEffect::class, EditMarkdownMessageEffectHandler())
    }
