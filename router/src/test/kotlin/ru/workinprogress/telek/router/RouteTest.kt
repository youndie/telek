package ru.workinprogress.telek.router

import kotlinx.serialization.Serializable
import org.junit.jupiter.api.Assertions.assertTrue
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

@Serializable
@RouteContext("purchase", "category")
data class PurchaseCategory(
    val id: String,
    val page: Int,
) : Route

@Serializable
@RouteContext("purchase", "product")
data class PurchaseProduct(
    val id: String,
) : Route

class RouteTest {
    @Serializable
    data class ExampleRoute(
        val id: String,
        val page: Int,
    )

    @Test
    fun `encodeParams produces compact underscore format`() {
        val encoded = encodeParams(ExampleRoute("123", 2))
        assertEquals("id_123_page_2", encoded)
    }

    @Test
    fun `decodeParams reconstructs original object`() {
        val params = "id_123_page_2"
        val decoded = decodeParams<ExampleRoute>(params)
        assertEquals(ExampleRoute("123", 2), decoded)
    }

    @Test
    fun `parseCommonRoute splits valid strings correctly`() {
        val raw = "purchase:category:id_123_page_2"
        val route = parseCommonRoute(raw)
        assertEquals("purchase", route.scope)
        assertEquals("category", route.action)
        assertEquals("id_123_page_2", route.params)
    }

    @Test
    fun `parseCommonRoute throws on invalid input`() {
        assertFailsWith<IllegalArgumentException> {
            parseCommonRoute("badformat")
        }
    }

    @Test
    fun `encodeRoute produces expected string`() {
        val encoded = RouteUtils.encodeRoute(PurchaseCategory("x42", 3))
        assertEquals("purchase:category:id_x42_page_3", encoded)
    }

    @Test
    fun `decodeRoute reconstructs object correctly`() {
        val raw = "purchase:category:id_777_page_9"
        val decoded = RouteUtils.decodeRoute<PurchaseCategory>(raw)
        assertEquals("777", decoded.id)
        assertEquals(9, decoded.page)
    }

    @Test
    fun `RouteRegistry registers and decodes routes automatically`() {
        val routeRegistry = RouteRegistry()
        routeRegistry.register<PurchaseCategory>()
        routeRegistry.register<PurchaseProduct>()

        val category = PurchaseCategory("x42", 3)
        val encodedCategory = RouteUtils.encodeRoute(category)
        val decodedCategory = routeRegistry.decode<Route>(encodedCategory)

        assertTrue(routeRegistry.canDecode(encodedCategory))
        assertIs<PurchaseCategory>(decodedCategory)
        assertEquals(category, decodedCategory)

        val product = PurchaseProduct("p1")
        val encodedProduct = RouteUtils.encodeRoute(product)
        val decodedProduct = routeRegistry.decode<Route>(encodedProduct)

        assertTrue(routeRegistry.canDecode(encodedProduct))
        assertIs<PurchaseProduct>(decodedProduct)
        assertEquals(product, decodedProduct)
    }

    @Test
    fun `RouteRegistry throws for unregistered route`() {
        val raw = "unknown:thing:id_99"
        assertFailsWith<IllegalStateException> {
            RouteRegistry().decode<Route>(raw)
        }
    }

    @Serializable
    data class TestPurchaseCategory(
        val id: String,
        val page: Int,
        val active: Boolean,
    )

    private fun randomCategory(): TestPurchaseCategory =
        TestPurchaseCategory(
            id = (1..5).joinToString("") { ('a'..'z').random().toString() },
            page = Random.nextInt(0, 100),
            active = Random.nextBoolean(),
        )

    @Test
    fun `encodeParams and decodeParams are reversible for multiple samples`() {
        repeat(500) {
            val original = randomCategory()
            val encoded = encodeParams(original)
            val decoded = decodeParams<TestPurchaseCategory>(encoded)
            assertEquals(original, decoded, "Failed at sample #$it -> encoded: $encoded")
        }
    }

    @Test
    fun `encodeParams is deterministic`() {
        val obj = TestPurchaseCategory("abc", 42, true)
        val first = encodeParams(obj)
        val second = encodeParams(obj)
        assertEquals(first, second)
    }

    @Test
    fun `decodeParams fails on malformed string`() {
        assertFailsWith<Exception> {
            decodeParams<TestPurchaseCategory>("id_123_page") // нечетное количество токенов
        }
    }
}
