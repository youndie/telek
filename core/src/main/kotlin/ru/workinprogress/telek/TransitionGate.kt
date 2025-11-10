package ru.workinprogress.telek

interface TransitionGate<S : State> {
    fun post(
        chatId: Long,
        reducer: (S) -> TransitionResult<S>,
    )
}

class TelekTransitionGate<S : State>(
    private val telek: Telek,
) : TransitionGate<S> {
    override fun post(
        chatId: Long,
        reducer: (S) -> TransitionResult<S>,
    ) {
        telek.applyReducer(chatId, reducer)
    }
}
