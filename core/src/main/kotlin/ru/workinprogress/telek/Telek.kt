package ru.workinprogress.telek

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class Telek(
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default),
    private val userStateStore: UserStateStore = DefaultUserStateStore(),
    private val dispatchers: List<StateDispatcher<out State>>,
    private val stateProvider: StateProvider = StateProvider { EmptyState },
    private val interceptors: List<TelekInterceptor> = emptyList(),
    private val effectExecutor: EffectExecutor,
) {
    private lateinit var context: ExecutionContext

    init {
        dispatchers.forEach { registerDispatcher(it) }
    }

    fun initIfNeeded(context: ExecutionContext) {
        if (::context.isInitialized) return
        this.context = context
    }

    fun onInput(
        chatId: Long,
        input: Input,
    ) {
        scope.launch {
            interceptors.forEach { it.onBeforeInput(chatId, input) }

            runCatching {
                var effectResults: List<EffectResult> = emptyList()
                var dispatcher: StateDispatcher<out State>? = null
                var newState: State? = null
                var oldState: State? = null

                userStateStore.update(chatId) { current ->
                    oldState = current
                    val state = current ?: stateProvider.initialState(chatId)
                    dispatcher = findDispatcher(state, input)
                    val transitionResult = dispatcher?.handle(state, input) ?: TransitionResult(state)
                    effectResults = effectExecutor.execute(context, transitionResult.effects)
                    newState = transitionResult.newState
                    transitionResult.newState
                }

                effectResults.forEach { result ->
                    dispatcher?.onEffectResult(newState ?: return@forEach, result)
                }

                interceptors.forEach { it.onAfterStateChanged(chatId, oldState, newState!!) }
            }.onFailure { e ->
                interceptors.forEach { it.onError(chatId, input, e) }
            }
        }
    }

    fun <S : State> applyReducer(
        chatId: Long,
        reducer: (S) -> TransitionResult<S>,
    ) {
        scope.launch {
            runCatching {
                userStateStore.update(chatId) { current ->
                    val state = current ?: stateProvider.initialState(chatId)

                    @Suppress("UNCHECKED_CAST")
                    val transitionResult = reducer(state as S)
                    val effectResults = effectExecutor.execute(context, transitionResult.effects)
                    val dispatcher = findDispatcher(state)
                    effectResults.forEach { result ->
                        dispatcher?.onEffectResult(transitionResult.newState, result)
                    }
                    interceptors.forEach { it.onAfterStateChanged(chatId, state, transitionResult.newState) }
                    transitionResult.newState
                }
            }.onFailure { e ->
                interceptors.forEach { it.onError(chatId, null, e) }
            }
        }
    }

    private fun registerDispatcher(dispatcher: StateDispatcher<out State>) {
        effectExecutor.let { executor ->
            dispatcher.attach(
                transitionGate = TelekTransitionGate(this),
            )
        }
    }

    private fun findDispatcher(state: State?): StateDispatcher<out State>? =
        state?.let { s -> dispatchers.firstOrNull { it.stateClass.isInstance(s) } }

    private fun findDispatcher(
        state: State?,
        input: Input,
    ): StateDispatcher<out State>? {
        if (input is Input.Message && input.text.startsWith("/")) {
            val cmd = input.text.removePrefix("/")
            return dispatchers.firstOrNull { it.startCommand == cmd }
                ?: dispatchers.firstOrNull { it.startCommand == "*" } // global
        }

        return state?.let { s -> dispatchers.firstOrNull { it.stateClass.isInstance(s) } }
    }
}
