package io.github.youndie.telek.docs

import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import io.github.youndie.telek.ConversationKey
import io.github.youndie.telek.Keying
import io.github.youndie.telek.Telek
import io.github.youndie.telek.ktg.KtgContextSource
import io.github.youndie.telek.ktg.connect

// Compiles the README's "What a conversation is keyed by" section.

@Suppress("unused")
private val onePerson: ConversationKey = ConversationKey.chatAndUser(chatId = -1001234567890, userId = 42)

@Suppress("unused")
private val wholeChat: ConversationKey = ConversationKey.chat(chatId = -1001234567890)

@Suppress("unused")
private fun BehaviourContext.keyingSample(
    telek: Telek,
    contextSource: KtgContextSource,
) {
    connect(telek, contextSource, Keying.PerUserInChat)
    connect(telek, contextSource, Keying.PerChat)
}
