// Compiled copy of README.md's "Usage with Telegram bot" test snippet — keep both in sync.
package io.github.youndie.telek.docs

import io.github.youndie.telek.Callback
import io.github.youndie.telek.ktg.effect.SendMessageEffect
import io.github.youndie.telek.router.RouteUtils
import kotlin.test.Test
import kotlin.test.assertEquals

class ExampleDispatcherTest {
    private val dispatcher = ExampleDispatcher()

    private fun answer(route: io.github.youndie.telek.router.Route) =
        dispatcher.transition(
            state = ExampleState.Confirming(number = 1, string = "hello"),
            input = Callback(chatId = 42, messageId = 7, data = RouteUtils.encodeRouteDynamic(route)),
        )

    @Test
    fun `confirming ends the flow and says so`() {
        val result = answer(ExampleConfirm())

        assertEquals(ExampleState.Done, result.newState)
        assertEquals(
            "confirmed",
            result.effects
                .filterIsInstance<SendMessageEffect>()
                .single()
                .text,
        )
    }

    // The state is Done either way, so asserting only on it would pass with the two routes swapped.
    // The reply is the only thing that tells them apart, which makes it the thing worth asserting.
    @Test
    fun `cancelling ends the flow and says the other thing`() {
        val result = answer(ExampleCancel())

        assertEquals(ExampleState.Done, result.newState)
        assertEquals(
            "canceled",
            result.effects
                .filterIsInstance<SendMessageEffect>()
                .single()
                .text,
        )
    }
}
