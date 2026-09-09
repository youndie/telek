package io.github.youndie.telek

@DslMarker
public annotation class WizardDsl

public inline fun <S : State> transition(block: TransitionBuilder<S>.() -> Unit): TransitionResult<S> =
    TransitionBuilder<S>().apply(block).build()

public fun <S : State> noTransition(state: S): TransitionResult<S> = TransitionResult(state)

@WizardDsl
public class TransitionBuilder<S : State> {
    private val effects = mutableListOf<Effect>()
    public lateinit var newState: S

    public fun add(effect: Effect) {
        effects += effect
    }

    public fun build(): TransitionResult<S> {
        check(::newState.isInitialized) {
            "TransitionBuilder.newState was never set — did you forget `newState = ...` inside " +
                "this `transition { }` block? (${effects.size} effect(s) were added before this failed)"
        }
        return TransitionResult(newState, effects)
    }
}

public data class TransitionResult<S : State>(
    val newState: S,
    val effects: List<Effect> = emptyList(),
)
