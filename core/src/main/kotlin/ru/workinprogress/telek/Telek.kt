package ru.workinprogress.telek

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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
                var dispatcher: StateDispatcher<out State>? = null
                var effectResults: List<EffectResult> = emptyList()
                var oldState: State? = null
                var newState: State? = null

                userStateStore.update(chatId) { current ->
                    oldState = current
                    val state = current ?: initialStateProvider.initialState(chatId)
                    val computation = reducerProvider(state)
                    val transitionResult = computation.transitionResult
                    val foundDispatcher = computation.dispatcher
                    dispatcher = foundDispatcher

                    effectResults = effectExecutor.execute(context, transitionResult.effects)
                    newState = transitionResult.newState
                    transitionResult.newState
                }

                newState?.let { s ->
                    effectResults.forEach { result ->
                        dispatcher?.onEffectResult(s, result)
                    }
                    interceptors.forEach { it.onAfterStateChanged(chatId, oldState, s) }
                }
            }.onFailure { e ->
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

    fun <S : State> applyReducer(
        chatId: Long,
        reducer: (S) -> TransitionResult<S>,
    ) {
        processTransition(chatId, null) { state ->
            @Suppress("UNCHECKED_CAST")
            val transitionResult = reducer(state as S)
            val dispatcher = findDispatcherStrategy.findDispatcher(state)
            TransitionComputation(transitionResult, dispatcher)
        }
    }

    private fun registerDispatcher(dispatcher: StateDispatcher<out State>) {
        effectExecutor.let { executor ->
            dispatcher.attach(
                transitionGate = TelekTransitionGate(this),
            )
        }
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
        if (input != null && input is Input.Message && input.text.startsWith("/")) {
            val cmd = input.text.removePrefix("/")
            return dispatchers.firstOrNull { it.startCommand == cmd }
                ?: dispatchers.firstOrNull { it.startCommand == "*" }
        }

        return state?.let { s -> dispatchers.firstOrNull { it.stateClass.isInstance(s) } }
    }
}

interface FindDispatcherStrategy {
    fun findDispatcher(
        state: State?,
        input: Input? = null,
    ): StateDispatcher<out State>?
}
