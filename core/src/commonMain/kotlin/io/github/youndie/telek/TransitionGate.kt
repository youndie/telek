package io.github.youndie.telek

import kotlin.reflect.KClass

public interface TransitionGate<S : State> {
    public fun post(
        chatId: Long,
        reducer: (S) -> TransitionResult<S>,
    )
}

// `internal`, not `public`: `Telek` constructs one for each dispatcher it attaches and never
// hands it back. Nothing outside this module names the type, so publishing it would only promise
// a constructor nobody calls. The port it implements, `TransitionGate`, stays public.
internal class TelekTransitionGate<S : State>(
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
