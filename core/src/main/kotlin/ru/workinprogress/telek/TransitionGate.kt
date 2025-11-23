package ru.workinprogress.telek

import kotlin.reflect.KClass

interface TransitionGate<S : State> {
    fun post(
        chatId: Long,
        reducer: (S) -> TransitionResult<S>,
    )
}

class TelekTransitionGate<S : State>(
    private val telek: Telek,
    private val kClass: KClass<S>,
) : TransitionGate<S> {
    override fun post(
        chatId: Long,
        reducer: (S) -> TransitionResult<S>,
    ) {
        telek.applyReducer(chatId, kClass, reducer)
    }
}
