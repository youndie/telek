package ru.workinprogress.telek

@DslMarker
annotation class WizardDsl

inline fun <S : State> transition(block: TransitionBuilder<S>.() -> Unit): TransitionResult<S> = TransitionBuilder<S>().apply(block).build()

fun <S : State> noTransition(state: S) = TransitionResult(state)

@WizardDsl
class TransitionBuilder<S : State> {
    private val effects = mutableListOf<Effect>()
    lateinit var newState: S

    fun add(effect: Effect) {
        effects += effect
    }

    fun build(): TransitionResult<S> {
        check(::newState.isInitialized) {
            "TransitionBuilder.newState was never set — did you forget `newState = ...` inside " +
                "this `transition { }` block? (${effects.size} effect(s) were added before this failed)"
        }
        return TransitionResult(newState, effects)
    }
}

data class TransitionResult<S : State>(
    val newState: S,
    val effects: List<Effect> = emptyList(),
)
