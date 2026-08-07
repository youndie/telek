package ru.workinprogress.telek.router

import kotlinx.serialization.Serializable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class EncodeRouteDynamicTest {
    @Serializable
    @RouteContext(scope = "purchase", action = "category")
    data class PurchaseCategory(
        val id: String,
        val page: Int,
    ) : Route

    @Serializable
    @RouteContext(scope = "shop", action = "product")
    data class ShopProduct(
        val name: String,
        val price: Double,
        val available: Boolean,
    ) : Route

    @Serializable
    data class NoAnnotationRoute(
        val value: String,
    ) : Route

    @Test
    fun `encodeRouteDynamic encodes simple route`() {
        val route = PurchaseCategory(id = "A123", page = 2)
        val encoded = RouteUtils.encodeRouteDynamic(route)
        assertEquals("purchase:category:id_A123_page_2", encoded)
    }

    @Test
    fun `encodeRouteDynamic encodes multiple fields with underscores`() {
        val route = ShopProduct(name = "Bread", price = 1.99, available = true)
        val encoded = RouteUtils.encodeRouteDynamic(route)
        assertEquals("shop:product:name_Bread_price_1.99_available_true", encoded)
    }

    @Test
    fun `encodeRouteDynamic is deterministic`() {
        val route = PurchaseCategory("XYZ", 5)
        val encoded1 = RouteUtils.encodeRouteDynamic(route)
        val encoded2 = RouteUtils.encodeRouteDynamic(route)
        assertEquals(encoded1, encoded2)
    }

    @Test
    fun `encodeRouteDynamic fails when annotation is missing`() {
        val route = NoAnnotationRoute("oops")
        assertFailsWith<IllegalStateException> {
            RouteUtils.encodeRouteDynamic(route)
        }
    }
}
