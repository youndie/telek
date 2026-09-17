package io.github.youndie.telek

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException
import kotlin.reflect.KClass
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

public class Telek(
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
     * Hands the transition off to that conversation's worker instead of running it here, so that
     * all transitions for one [ConversationKey] — whether triggered by [onInput] or
     * [applyReducer] — execute strictly in submission order, one at a time. See [ChatWorkers].
     */
    private fun processTransition(
        key: ConversationKey,
        input: Input?,
        reducerProvider: (State) -> TransitionComputation,
    ) {
        scope.launch {
            chatWorkers.submit(key) {
                runTransition(key, input, reducerProvider)
            }
        }
    }

    private suspend fun runTransition(
        key: ConversationKey,
        input: Input?,
        reducerProvider: (State) -> TransitionComputation,
    ) {
        if (input != null) interceptors.forEach { it.onBeforeInput(key, input) }

        // `try` and not `runCatching`: the cancellation was already rethrown below, but from
        // inside `onFailure`, which is a shape no reader -- and no rule -- can check at a
        // glance. The behaviour is unchanged; what changed is that it is now visible.
        try {
            val result =
                userStateStore.update(key) { current ->
                    val state = current ?: initialStateProvider.initialState(key)
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
                effectExecutor.execute(result.effects) { debounceKey, asyncWork ->
                    chatWorkers.launchAsync(key, debounceKey) {
                        val event = asyncWork()
                        if (event != null) onEvent(key, event)
                    }
                }
            effectResults.filterIsInstance<EffectFailed>().forEach { failed ->
                interceptors.forEach { it.onError(key, input, failed.error) }
            }
            result.dispatcher?.onEffectResults(result.newState, effectResults)
            interceptors.forEach {
                it.onAfterStateChanged(key, result.oldState, result.newState)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            interceptors.forEach { it.onError(key, input, e) }
        }
    }

    /**
     * Feeds an input into the conversation identified by [key].
     *
     * The key is a parameter and not read off [input] on purpose: `input.chatId` is the Telegram
     * address a reply goes to, which in a group is shared by everyone in it. See
     * [ConversationKey], and [Keying] for how the bundled transports derive one.
     */
    public fun onInput(
        key: ConversationKey,
        input: Input,
    ) {
        processTransition(key, input) { state ->
            val dispatcher = findDispatcherStrategy.findDispatcher(state, input)
            val transitionResult = dispatcher?.handle(state, input) ?: TransitionResult(state)
            TransitionComputation(transitionResult, dispatcher)
        }
    }

    /**
     * Routes an [Event] produced by an [AsyncEffectHandler] back into the FSM, purely by the
     * conversation's current state (an event is never the first message of a flow, so there's no
     * command/callback matching here — see [FindDispatcherStrategy]). Runs through the same
     * worker as [onInput]/[applyReducer], so ordering with regular inputs is preserved.
     *
     * The key is the one the async work was launched for, not [Event.chatId] — that, like
     * [Input.chatId], is an address.
     */
    private fun onEvent(
        key: ConversationKey,
        event: Event,
    ) {
        processTransition(key, null) { state ->
            val dispatcher = findDispatcherStrategy.findDispatcher(state)
            val transitionResult = dispatcher?.handleEvent(state, event) ?: TransitionResult(state)
            TransitionComputation(transitionResult, dispatcher)
        }
    }

    internal fun <S : State> applyReducer(
        key: ConversationKey,
        expectedStateType: KClass<S>,
        reducer: (S) -> TransitionResult<S>,
    ) {
        processTransition(key, null) { state ->
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

public class DefaultFindDispatcherStrategy(
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

public data class UpdateResult(
    val oldState: State?,
    val newState: State,
    val effects: List<Effect>,
    val dispatcher: StateDispatcher<out State>?,
)

public interface FindDispatcherStrategy {
    public fun findDispatcher(
        state: State?,
        input: Input? = null,
    ): StateDispatcher<out State>?
}
