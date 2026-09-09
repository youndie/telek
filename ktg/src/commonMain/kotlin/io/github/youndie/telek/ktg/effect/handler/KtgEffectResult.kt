package io.github.youndie.telek.ktg.effect.handler

import io.github.youndie.telek.EffectResult

public abstract class KtgEffectSuccess : EffectResult {
    public abstract val chatId: Long
    public abstract val messageId: Long
}

public class SendMessageEffectResult(
    override val chatId: Long,
    override val messageId: Long,
) : KtgEffectSuccess()

public class EditMessageEffectResult(
    override val chatId: Long,
    override val messageId: Long,
) : KtgEffectSuccess()

public class EditMarkupEffectResult(
    override val chatId: Long,
    override val messageId: Long,
) : KtgEffectSuccess()
