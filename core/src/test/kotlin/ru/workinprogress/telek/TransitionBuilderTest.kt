package ru.workinprogress.telek

import ru.workinprogress.telek.support.TestEffect
import ru.workinprogress.telek.support.TestState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TransitionBuilderTest {
    @Test
    fun `transition without effects produces empty effects list`() {
        val result =
            transition<TestState> {
                newState = TestState.Waiting(1)
            }

        assertEquals(TestState.Waiting(1), result.newState)
        assertTrue(result.effects.isEmpty())
    }

    @Test
    fun `add accumulates effects in order`() {
        val result =
            transition<TestState> {
                newState = TestState.Waiting(1)
                add(TestEffect("first"))
                add(TestEffect("second"))
                add(TestEffect("third"))
            }

        assertEquals(
            listOf(TestEffect("first"), TestEffect("second"), TestEffect("third")),
            result.effects,
        )
    }

    @Test
    fun `noTransition returns same state without effects`() {
        val state = TestState.Confirming(5)

        val result = noTransition(state)

        assertEquals(state, result.newState)
        assertTrue(result.effects.isEmpty())
    }
}
