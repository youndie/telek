@file:OptIn(ExperimentalSerializationApi::class, InternalSerializationApi::class)

package io.github.youndie.telek.router

import io.github.youndie.telek.Callback
import io.github.youndie.telek.router.RouteUtils.requireContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.SerialInfo
import kotlinx.serialization.properties.Properties
import kotlinx.serialization.properties.decodeFromStringMap
import kotlinx.serialization.properties.encodeToStringMap

public inline fun <reified T : Route> Callback.isRouteOf(registry: RouteRegistry): Boolean = registry.typeIs<T>(data)

public inline fun <reified T : Route> Callback.tryDecode(registry: RouteRegistry): T? =
    if (registry.typeIs<T>(data)) registry.decode(data) else null

/**
 * Marks a [Route] with the `scope:action` pair its encoded callback data starts with.
 *
 * [SerialInfo] is what makes this multiplatform: the serialization compiler plugin copies the
 * annotation into the class's generated `SerialDescriptor`, so telek reads it without
 * `KClass.annotations` (JVM-only reflection). The class must therefore be `@Serializable` — even
 * a route with no properties at all.
 */
@SerialInfo
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
public annotation class RouteContext(
    val scope: String,
    val action: String,
)

public interface Route {
    public fun encode(): String = RouteUtils.encodeRouteDynamic(this)
}

public data class CommonRoute(
    val scope: String,
    val action: String,
    val params: String? = null,
)

public fun parseCommonRoute(raw: String): CommonRoute {
    val parts = raw.split(":", limit = 3)
    require(parts.size >= 2) { "Invalid route string: $raw" }
    return CommonRoute(
        scope = parts[0],
        action = parts[1],
        params = parts.getOrNull(2),
    )
}

public inline fun <reified T : Any> encodeParams(instance: T): String {
    val map = Properties.encodeToStringMap(instance)
    return map.entries.joinToString("_") { "${it.key}_${it.value}" }
}

public inline fun <reified T : Any> decodeParams(params: String?): T {
    require(!params.isNullOrBlank()) { "Empty params for ${T::class.simpleName}" }
    val map =
        params
            .split("_")
            .chunked(2)
            .filter { it.size == 2 }
            .associate { (k, v) -> k to v }
    return Properties.decodeFromStringMap(map)
}

public fun interface RouteDecoder<T : Route> {
    public fun decode(raw: String): T
}

public fun routes(block: RouteRegistry.() -> Unit): RouteRegistry = RouteRegistry().apply(block)

public class RouteRegistry {
    // `@PublishedApi internal` and not `public`: nothing outside this file names it, and the only
    // reason it cannot simply be `internal` is that the `register` functions below are `inline` —
    // an inline body may only touch declarations at least as visible as itself. This keeps the
    // registry's storage out of the API while leaving those functions able to fill it.
    @PublishedApi
    internal val decoders: MutableMap<Pair<String, String>, RouteDecoder<out Route>> = mutableMapOf()

    public inline fun <reified T : Route> register(noinline decoder: (String) -> T) {
        val (scope, action) = requireContext<T>()
        decoders[scope to action] = RouteDecoder(decoder)
    }

    public inline fun <reified T : Route> register() {
        val (scope, action) = requireContext<T>()
        decoders[scope to action] = RouteDecoder { raw -> RouteUtils.decodeRoute<T>(raw) }
    }

    public inline fun <reified T : Route> typeIs(raw: String): Boolean {
        if (!canDecode(raw)) return false
        val (rawScope, rawAction) = parseCommonRoute(raw)
        val (scope, action) = requireContext<T>()
        return rawScope == scope && rawAction == action
    }

    @Suppress("UNCHECKED_CAST")
    public fun <T : Route> decode(raw: String): T {
        val (scope, action) = parseCommonRoute(raw)
        val decoder =
            decoders[scope to action]
                ?: error("No decoder registered for $scope:$action")
        return decoder.decode(raw) as T
    }

    public fun canDecode(raw: String): Boolean =
        runCatching {
            parseCommonRoute(raw)
        }.fold({ (scope, action) ->
            decoders.containsKey(scope to action)
        }, { false })

    internal fun clear() = decoders.clear()
}
