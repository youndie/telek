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
                userStateStore.update(chatId) { current ->
                    val state = current ?: stateProvider.initialState(chatId)
                    val dispatcher = findDispatcher(state, input)
                    val newState = dispatcher?.handle(context, state, input) ?: state
                    interceptors.forEach { it.onAfterStateChanged(chatId, current, newState) }
                    newState
                }
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
                    val result = reducer(state as S)
                    effectExecutor.execute(context, result.effects)
                    interceptors.forEach { it.onAfterStateChanged(chatId, state, result.newState) }
                    result.newState
                }
            }.onFailure { e ->
                interceptors.forEach { it.onError(chatId, null, e) }
            }
        }
    }

    private fun registerDispatcher(dispatcher: StateDispatcher<out State>) {
        effectExecutor.let { executor ->
            dispatcher.attach(
                effectExecutor = executor,
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
