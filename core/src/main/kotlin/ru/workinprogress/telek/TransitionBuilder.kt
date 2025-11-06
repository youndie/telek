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

    fun build(): TransitionResult<S> = TransitionResult(newState, effects)
}

data class TransitionResult<S : State>(
    val newState: S,
    val effects: List<Effect> = emptyList(),
)
