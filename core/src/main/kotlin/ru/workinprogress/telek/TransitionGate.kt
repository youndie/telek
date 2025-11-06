package ru.workinprogress.telek

interface TransitionGate {
    suspend fun post(
        chatId: Long,
        reducer: (State) -> TransitionResult<State>,
    )
}

class TelekTransitionGate(
    private val telek: Telek,
    private val context: ExecutionContext,
) : TransitionGate {
    override suspend fun post(
        chatId: Long,
        reducer: (State) -> TransitionResult<State>,
    ) {
        telek.applyReducer(context, chatId, reducer)
    }
}
