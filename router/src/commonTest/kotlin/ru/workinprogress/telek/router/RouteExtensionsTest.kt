package ru.workinprogress.telek.router

import kotlinx.serialization.Serializable
import ru.workinprogress.telek.Callback
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RouteExtensionsTest {
    @Serializable
    @RouteContext(scope = "ext", action = "one")
    data class ExtRouteOne(
        val id: String,
    ) : Route

    @Serializable
    @RouteContext(scope = "ext", action = "two")
    data class ExtRouteTwo(
        val id: String,
    ) : Route

    private val registry =
        routes {
            register<ExtRouteOne>()
            register<ExtRouteTwo>()
        }

    @Test
    fun `isRouteOf is true only for the matching route type`() {
        val callback = Callback(chatId = 1, messageId = 1, data = ExtRouteOne("a").encode())

        assertTrue(callback.isRouteOf<ExtRouteOne>(registry))
        assertFalse(callback.isRouteOf<ExtRouteTwo>(registry))
    }

    @Test
    fun `tryDecode returns the decoded route for a matching type`() {
        val route = ExtRouteOne("abc")
        val callback = Callback(1, 1, route.encode())

        assertEquals(route, callback.tryDecode<ExtRouteOne>(registry))
    }

    @Test
    fun `tryDecode returns null without throwing for a non-matching type`() {
        val callback = Callback(1, 1, ExtRouteOne("abc").encode())

        assertNull(callback.tryDecode<ExtRouteTwo>(registry))
    }

    @Test
    fun `Route encode default method matches encodeRouteDynamic`() {
        val route = ExtRouteOne("m")

        assertEquals(RouteUtils.encodeRouteDynamic(route), route.encode())
    }
}
