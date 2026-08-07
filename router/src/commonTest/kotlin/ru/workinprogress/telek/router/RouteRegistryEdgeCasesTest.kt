package ru.workinprogress.telek.router

import kotlinx.serialization.Serializable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class RouteRegistryEdgeCasesTest {
    @Serializable
    @RouteContext(scope = "wizard", action = "confirm")
    class ConfirmRoute : Route {
        override fun equals(other: Any?): Boolean = other is ConfirmRoute

        override fun hashCode(): Int = ConfirmRoute::class.hashCode()
    }

    @Serializable
    @RouteContext(scope = "shop", action = "item")
    data class ShopItem(
        val id: String,
        val price: Double,
        val featured: Boolean,
        val quantity: Int,
    ) : Route

    @Test
    fun `register with a custom decoder is used instead of the reflective one`() {
        val registry = RouteRegistry()
        var decoderInvocations = 0
        registry.register<ShopItem> { raw ->
            decoderInvocations++
            ShopItem(id = "custom", price = 0.0, featured = false, quantity = 0)
        }

        val decoded = registry.decode<ShopItem>("shop:item:id_a_price_1.0_featured_true_quantity_1")

        assertEquals(1, decoderInvocations)
        assertEquals(ShopItem("custom", 0.0, false, 0), decoded)
    }

    @Test
    fun `typeIs is false for an undecodable raw string without throwing`() {
        val registry = RouteRegistry()
        registry.register<ShopItem>()

        assertFalse(registry.typeIs<ShopItem>("not-a-route-at-all"))
    }

    @Test
    fun `decoding a route with no fields fails because params are empty`() {
        // ConfirmRoute has no properties, so encode() produces "wizard:confirm:" with empty
        // params. isRouteOf/canDecode work off scope+action alone and succeed, but decode()
        // goes through decodeParams which rejects blank params - this documents that limitation.
        val registry = RouteRegistry()
        registry.register<ConfirmRoute>()
        val encoded = ConfirmRoute().encode()

        assertTrue(registry.canDecode(encoded))
        assertTrue(registry.typeIs<ConfirmRoute>(encoded))
        assertFailsWith<IllegalArgumentException> {
            registry.decode<ConfirmRoute>(encoded)
        }
    }

    @Test
    fun `canDecode is false for a raw string without a colon`() {
        val registry = RouteRegistry()
        registry.register<ShopItem>()

        assertFalse(registry.canDecode("malformed"))
    }

    @Test
    fun `registry round-trips a route with mixed field types`() {
        val registry = RouteRegistry()
        registry.register<ShopItem>()
        val item = ShopItem(id = "sku-1", price = 12.5, featured = true, quantity = 3)

        val encoded = RouteUtils.encodeRoute(item)
        val decoded = registry.decode<ShopItem>(encoded)

        assertIs<ShopItem>(decoded)
        assertEquals(item, decoded)
    }
}
