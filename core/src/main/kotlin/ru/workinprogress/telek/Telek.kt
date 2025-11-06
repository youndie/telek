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
) {
    fun onInput(
        context: ExecutionContext,
        chatId: Long,
        input: Input,
    ) {
        scope.launch {
            interceptors.forEach { it.onBeforeEvent(chatId, input) }

            runCatching {
                userStateStore.update(chatId) { current ->
                    val state = current ?: stateProvider.initialState(chatId)
                    val dispatcher = findDispatcher(state, input)
                    val newState = dispatcher?.handle(context, state, input) ?: state
                    interceptors.forEach { it.onAfterEvent(chatId, current, newState) }
                    newState
                }
            }.onFailure { e ->
                interceptors.forEach { it.onError(chatId, input, e) }
            }
        }
    }

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
