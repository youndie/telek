package ru.workinprogress.telek

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException
import kotlin.reflect.KClass
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

class Telek(
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default),
    private val userStateStore: UserStateStore = DefaultUserStateStore(),
    private val dispatchers: List<StateDispatcher<out State>>,
    private val initialStateProvider: InitialStateProvider = InitialStateProvider { EmptyState },
    private val interceptors: List<TelekInterceptor> = emptyList(),
    private val effectExecutor: EffectExecutor,
    private val findDispatcherStrategy: FindDispatcherStrategy = DefaultFindDispatcherStrategy(dispatchers),
    chatWorkerIdleTimeout: Duration = 15.minutes,
    chatInboxCapacity: Int = 64,
    logger: TelekLogger = TelekLogger.NoOp,
) {
    private val chatWorkers = ChatWorkers(scope, chatWorkerIdleTimeout, chatInboxCapacity, logger)

    init {
        dispatchers.forEach { registerDispatcher(it) }
    }

    /**
     * Hands the transition off to that chat's worker instead of running it here, so that all
     * transitions for one chatId — whether triggered by [onInput] or [applyReducer] — execute
     * strictly in submission order, one at a time. See [ChatWorkers].
     */
    private fun processTransition(
        chatId: Long,
        input: Input?,
        reducerProvider: (State) -> TransitionComputation,
    ) {
        scope.launch {
            chatWorkers.submit(chatId) {
                runTransition(chatId, input, reducerProvider)
            }
        }
    }

    private suspend fun runTransition(
        chatId: Long,
        input: Input?,
        reducerProvider: (State) -> TransitionComputation,
    ) {
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

            val effectResults =
                effectExecutor.execute(result.effects) { key, asyncWork ->
                    chatWorkers.launchAsync(chatId, key) {
                        val event = asyncWork()
                        if (event != null) onEvent(chatId, event)
                    }
                }
            effectResults.filterIsInstance<EffectFailed>().forEach { failed ->
                interceptors.forEach { it.onError(chatId, input, failed.error) }
            }
            result.dispatcher?.onEffectResults(result.newState, effectResults)
            interceptors.forEach {
                it.onAfterStateChanged(chatId, result.oldState, result.newState)
            }
        }.onFailure { e ->
            if (e is CancellationException) throw e
            interceptors.forEach { it.onError(chatId, input, e) }
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

    /**
     * Routes an [Event] produced by an [AsyncEffectHandler] back into the FSM, purely by the
     * chat's current state (an event is never the first message of a flow, so there's no
     * command/callback matching here — see [FindDispatcherStrategy]). Runs through the same
     * per-chat worker as [onInput]/[applyReducer], so ordering with regular inputs is preserved.
     */
    private fun onEvent(
        chatId: Long,
        event: Event,
    ) {
        processTransition(chatId, null) { state ->
            val dispatcher = findDispatcherStrategy.findDispatcher(state)
            val transitionResult = dispatcher?.handleEvent(state, event) ?: TransitionResult(state)
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
