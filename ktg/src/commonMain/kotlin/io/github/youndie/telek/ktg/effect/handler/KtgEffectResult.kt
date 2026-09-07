package io.github.youndie.telek.ktg.effect.handler

import io.github.youndie.telek.EffectResult

abstract class KtgEffectSuccess : EffectResult {
    abstract val chatId: Long
    abstract val messageId: Long
}

class SendMessageEffectResult(
    override val chatId: Long,
    override val messageId: Long,
) : KtgEffectSuccess()

class EditMessageEffectResult(
    override val chatId: Long,
    override val messageId: Long,
) : KtgEffectSuccess()

class EditMarkupEffectResult(
    override val chatId: Long,
    override val messageId: Long,
) : KtgEffectSuccess()
