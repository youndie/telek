package ru.workinprogress.telek.router

import kotlinx.serialization.Serializable
import ru.workinprogress.telek.Input
import ru.workinprogress.telek.State
import ru.workinprogress.telek.TransitionResult
import ru.workinprogress.telek.noTransition
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class StateDispatcherWithRoutesTest {
    @Serializable
    @RouteContext(scope = "wizard", action = "next")
    data class NextRoute(
        val step: Int,
    ) : Route

    data class DummyState(
        val value: Int = 0,
    ) : State

    private class TestDispatcher : StateDispatcherWithRoutes<DummyState>() {
        override val startCommand = "wizard"
        override val stateClass = DummyState::class
        override val routeRegistry =
            routes {
                register<NextRoute>()
            }

        override fun transition(
            state: DummyState,
            input: Input,
        ): TransitionResult<DummyState> = noTransition(state)
    }

    @Test
    fun `canHandleCallback is true when data equals startCommand`() {
        val dispatcher = TestDispatcher()

        assertTrue(dispatcher.canHandleCallback("wizard"))
    }

    @Test
    fun `canHandleCallback is true when the route registry can decode the data`() {
        val dispatcher = TestDispatcher()
        val encoded = NextRoute(step = 2).encode()

        assertTrue(dispatcher.canHandleCallback(encoded))
    }

    @Test
    fun `canHandleCallback is false when neither startCommand nor a registered route matches`() {
        val dispatcher = TestDispatcher()

        assertFalse(dispatcher.canHandleCallback("unrelated:data:x_1"))
    }
}
