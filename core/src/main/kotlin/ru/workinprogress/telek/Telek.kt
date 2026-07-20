package ru.workinprogress.telek

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException
import kotlin.reflect.KClass

class Telek(
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default),
    private val userStateStore: UserStateStore = DefaultUserStateStore(),
    private val dispatchers: List<StateDispatcher<out State>>,
    private val initialStateProvider: InitialStateProvider = InitialStateProvider { EmptyState },
    private val interceptors: List<TelekInterceptor> = emptyList(),
    private val effectExecutor: EffectExecutor,
    private val findDispatcherStrategy: FindDispatcherStrategy = DefaultFindDispatcherStrategy(dispatchers),
) {
    private lateinit var context: ExecutionContext

    init {
        dispatchers.forEach { registerDispatcher(it) }
    }

    fun initIfNeeded(context: ExecutionContext) {
        if (::context.isInitialized) return
        this.context = context
    }

    private fun processTransition(
        chatId: Long,
        input: Input?,
        reducerProvider: (State) -> TransitionComputation,
    ) {
        scope.launch {
            if (input != null) interceptors.forEach { it.onBeforeInput(chatId, input) }

            runCatching {
                val result =
                    userStateStore.update(chatId) { current ->
                        val state = current ?: initialStateProvider.initialState(chatId)
                        val computation = reducerProvider(state)
                        val transResult = computation.transitionResult

                        UpdateResult(
                            oldState = current,
                            newState = transResult.newState,
                            effects = transResult.effects,
                            dispatcher = computation.dispatcher,
                        )
                    }

                val effectResults = effectExecutor.execute(context, result.effects)
                result.dispatcher?.onEffectResults(result.newState, effectResults)
                interceptors.forEach {
                    it.onAfterStateChanged(chatId, result.oldState, result.newState)
                }
            }.onFailure { e ->
                if (e is CancellationException) throw e
                interceptors.forEach { it.onError(chatId, input, e) }
            }
        }
    }

    fun onInput(
        chatId: Long,
        input: Input,
    ) {
        processTransition(chatId, input) { state ->
            val dispatcher = findDispatcherStrategy.findDispatcher(state, input)
            val transitionResult = dispatcher?.handle(state, input) ?: TransitionResult(state)
            TransitionComputation(transitionResult, dispatcher)
        }
    }

    internal fun <S : State> applyReducer(
        chatId: Long,
        expectedStateType: KClass<S>,
        reducer: (S) -> TransitionResult<S>,
    ) {
        processTransition(chatId, null) { state ->
            @Suppress("UNCHECKED_CAST")
            if (expectedStateType.isInstance(state)) {
                val transitionResult = reducer(state as S)
                val dispatcher = findDispatcherStrategy.findDispatcher(state)
                TransitionComputation(transitionResult, dispatcher)
            } else {
                throw IllegalStateException("State mismatch: expected $expectedStateType but got ${state::class}")
            }
        }
    }

    private fun <T : State> registerDispatcher(dispatcher: StateDispatcher<T>) {
        dispatcher.attach(
            transitionGate = TelekTransitionGate(this, dispatcher.stateClass),
        )
    }

    private class TransitionComputation(
        val transitionResult: TransitionResult<out State>,
        val dispatcher: StateDispatcher<out State>?,
    )
}

class DefaultFindDispatcherStrategy(
    private val dispatchers: List<StateDispatcher<out State>>,
) : FindDispatcherStrategy {
    override fun findDispatcher(
        state: State?,
        input: Input?,
    ): StateDispatcher<out State>? {
        if (input != null && input is Message && input.text.startsWith("/")) {
            val cmd = input.text.removePrefix("/")
            return dispatchers.firstOrNull { it.startCommand == cmd }
                ?: dispatchers.firstOrNull { it.startCommand == "*" }
        }

        if (input is Callback) {
            dispatchers.firstOrNull { it.canHandleCallback(input.data) }?.let { return it }
        }

        return state?.let { s -> dispatchers.firstOrNull { it.stateClass.isInstance(s) } }
    }
}

data class UpdateResult(
    val oldState: State?,
    val newState: State,
    val effects: List<Effect>,
    val dispatcher: StateDispatcher<out State>?,
)

interface FindDispatcherStrategy {
    fun findDispatcher(
        state: State?,
        input: Input? = null,
    ): StateDispatcher<out State>?
}
