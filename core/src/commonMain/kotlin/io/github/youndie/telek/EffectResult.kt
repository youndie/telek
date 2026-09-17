package io.github.youndie.telek

public interface EffectResult

public object EffectSuccess : EffectResult

public class EffectFailed(
    public val error: Throwable,
) : EffectResult

/**
 * An [Effect] and what running it produced.
 *
 * The pairing lives here rather than as a field on [EffectResult] because [EffectSuccess] is an
 * `object` — there is one of it, shared by every effect that succeeded, so it has nowhere to put
 * "which effect". Pairing at the boundary also leaves every existing [EffectResult] implementation,
 * transport ones included, exactly as it was.
 *
 * Why it exists at all: [StateDispatcher.onEffectResults] used to receive a bare list of results,
 * and nothing connected it to the list of effects that produced them — not an index, not an id.
 * A dispatcher that sent two messages and needed the id of the *first* had to assume the two lists
 * line up positionally. That assumption is correct until somebody adds an effect to the transition,
 * and then it is silently wrong: the dispatcher edits the wrong message, and no test that asserts
 * on a single effect can see it.
 */
public data class EffectOutcome(
    public val effect: Effect,
    public val result: EffectResult,
)
