package ru.workinprogress.telek.router

import kotlinx.serialization.Serializable
import ru.workinprogress.telek.router.RouteRegistry
import kotlin.random.Random
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class RouteRegistryPropertiesTest {
    val routeRegistry = RouteRegistry()

    @Serializable
    @RouteContext(scope = "purchase", action = "category")
    data class PurchaseCategory(
        val id: String,
        val page: Int,
    ) : Route

    @Serializable
    @RouteContext(scope = "purchase", action = "product")
    data class PurchaseProduct(
        val id: String,
    ) : Route

    @BeforeTest
    fun setup() {
        routeRegistry.clear()
        routeRegistry.register<PurchaseCategory>()
        routeRegistry.register<PurchaseProduct>()
    }

    private fun randomCategory(): PurchaseCategory =
        PurchaseCategory(
            id = (1..4).joinToString("") { ('a'..'z').random().toString() },
            page = Random.nextInt(0, 10),
        )

    private fun randomProduct(): PurchaseProduct =
        PurchaseProduct(
            id = (1..5).joinToString("") { ('0'..'9').random().toString() },
        )

    @Test
    fun `RouteRegistry decodes exactly what was encoded (PurchaseCategory)`() {
        repeat(500) {
            val original = randomCategory()
            val encoded = RouteUtils.encodeRoute(original)
            val decoded = routeRegistry.decode<Route>(encoded)
            assertIs<PurchaseCategory>(decoded)
            assertEquals(original, decoded)
        }
    }

    @Test
    fun `RouteRegistry decodes exactly what was encoded (PurchaseProduct)`() {
        repeat(500) {
            val original = randomProduct()
            val encoded = RouteUtils.encodeRoute(original)
            val decoded = routeRegistry.decode<Route>(encoded)
            assertIs<PurchaseProduct>(decoded)
            assertEquals(original, decoded)
        }
    }

    @Test
    fun `RouteRegistry throws for unregistered action`() {
        val raw = "purchase:unknown:id_123"
        assertFailsWith<IllegalStateException> {
            routeRegistry.decode<Route>(raw)
        }
    }

    @Test
    fun `RouteRegistry recognizes decodable routes`() {
        val known = "purchase:product:id_55"
        val unknown = "shop:item:id_10"
        assertTrue(routeRegistry.canDecode(known))
        assertFalse(routeRegistry.canDecode(unknown))
    }
}
