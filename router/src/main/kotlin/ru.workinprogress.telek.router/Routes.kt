@file:OptIn(ExperimentalSerializationApi::class, InternalSerializationApi::class)

package ru.workinprogress.telek.router

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.properties.Properties
import kotlinx.serialization.properties.decodeFromStringMap
import kotlinx.serialization.properties.encodeToStringMap
import ru.workinprogress.telek.Input
import ru.workinprogress.telek.router.RouteUtils.requireContext
import ru.workinprogress.telek.telegram.RowBuilder

fun RowBuilder.callback(
    name: String,
    route: Route,
) {
    callback(name, route.encode())
}

inline fun <reified T : Route> Input.Callback.isRouteOf(registry: RouteRegistry): Boolean = registry.typeIs<T>(data)

inline fun <reified T : Route> Input.Callback.tryDecode(registry: RouteRegistry): T? =
    if (registry.typeIs<T>(data)) registry.decode(data) else null

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class RouteContext(
    val scope: String,
    val action: String,
)

interface Route {
    fun encode(): String = RouteUtils.encodeRouteDynamic(this)
}

data class CommonRoute(
    val scope: String,
    val action: String,
    val params: String? = null,
)

fun parseCommonRoute(raw: String): CommonRoute {
    val parts = raw.split(":", limit = 3)
    require(parts.size >= 2) { "Invalid route string: $raw" }
    return CommonRoute(
        scope = parts[0],
        action = parts[1],
        params = parts.getOrNull(2),
    )
}

inline fun <reified T : Any> encodeParams(instance: T): String {
    val map = Properties.encodeToStringMap(instance)
    return map.entries.joinToString("_") { "${it.key}_${it.value}" }
}

inline fun <reified T : Any> decodeParams(params: String?): T {
    require(!params.isNullOrBlank()) { "Empty params for ${T::class.simpleName}" }
    val map =
        params
            .split("_")
            .chunked(2)
            .filter { it.size == 2 }
            .associate { (k, v) -> k to v }
    return Properties.decodeFromStringMap(map)
}

fun interface RouteDecoder<T : Route> {
    fun decode(raw: String): T
}

fun routes(block: RouteRegistry.() -> Unit) = RouteRegistry().apply(block)

class RouteRegistry {
    val decoders = mutableMapOf<Pair<String, String>, RouteDecoder<out Route>>()

    inline fun <reified T : Route> register(noinline decoder: (String) -> T) {
        val (scope, action) = requireContext<T>()
        decoders[scope to action] = RouteDecoder(decoder)
    }

    inline fun <reified T : Route> register() {
        val (scope, action) = requireContext<T>()
        decoders[scope to action] = RouteDecoder { raw -> RouteUtils.decodeRoute<T>(raw) }
    }

    inline fun <reified T : Route> typeIs(raw: String): Boolean {
        if (!canDecode(raw)) return false
        val (rawScope, rawAction) = parseCommonRoute(raw)
        val (scope, action) = requireContext<T>()
        return rawScope == scope && rawAction == action
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : Route> decode(raw: String): T {
        val (scope, action) = parseCommonRoute(raw)
        val decoder =
            decoders[scope to action]
                ?: error("No decoder registered for $scope:$action")
        return decoder.decode(raw) as T
    }

    fun canDecode(raw: String): Boolean {
        val (scope, action) = parseCommonRoute(raw)
        return decoders.containsKey(scope to action)
    }

    internal fun clear() = decoders.clear()
}
