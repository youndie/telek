package ru.workinprogress.telek

/**
 * Handles an [Effect] that shouldn't block the transition that produced it — typically network
 * calls. Unlike [EffectHandler], this doesn't run as part of the current transition and doesn't
 * produce an [EffectResult]: it runs independently (cancelled if the chat goes idle, see
 * `ChatWorkers` in telek's implementation), and whatever [Event] it returns re-enters the FSM as
 * its own, later transition — handled by [StateDispatcher]'s `transition(state, event)` overload.
 *
 * Register with [EffectRegistry.registerAsync], not [EffectRegistry.register] — an effect class
 * should be registered as one or the other, never both.
 */
interface AsyncEffectHandler<E : Effect> {
    suspend fun handle(
        context: ExecutionContext,
        effect: E,
    ): Event?
}
