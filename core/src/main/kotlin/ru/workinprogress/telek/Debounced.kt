package ru.workinprogress.telek

/**
 * Opt-in marker for an [Effect] whose async dispatch should cancel any previous in-flight async
 * effect for the *same chat* with an equal [debounceKey], instead of letting them run
 * concurrently — e.g. a user clicking through categories quickly, where only the latest fetch's
 * result should matter.
 *
 * Only has an effect on effects registered via [EffectRegistry.registerAsync] (see
 * [AsyncEffectHandler]); ignored for synchronous effects. Not implementing this interface is the
 * default and means "run every dispatch of this effect independently" — cancellation is never
 * automatic or implicit for effects that don't opt in.
 */
interface Debounced {
    val debounceKey: Any
}
