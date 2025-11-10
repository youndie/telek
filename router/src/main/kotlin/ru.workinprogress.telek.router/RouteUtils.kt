package ru.workinprogress.telek.router

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.properties.Properties
import kotlinx.serialization.properties.encodeToStringMap
import kotlinx.serialization.serializer
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

@OptIn(ExperimentalSerializationApi::class, InternalSerializationApi::class)
object RouteUtils {
    private val serializerCache =
        ConcurrentHashMap<KClass<*>, KSerializer<Any>>()

    fun encodeRouteDynamic(route: Route): String {
        val (scope, action) =
            getRouteContext(route::class)
                ?: error("Missing @RouteContext for ${route::class.simpleName}")

        val serializer =
            serializerCache.getOrPut(route::class) {
                @Suppress("UNCHECKED_CAST")
                route::class.serializer() as KSerializer<Any>
            }

        val map = Properties.Default.encodeToStringMap(serializer, route)
        val paramsString = map.entries.joinToString("_") { "${it.key}_${it.value}" }
        return "$scope:$action:$paramsString"
    }

    inline fun <reified T : Route> encodeRoute(instance: T): String {
        val (scope, action) = requireContext<T>()
        val params = Properties.Default.encodeToStringMap(instance)
        val paramString = params.entries.joinToString("_") { "${it.key}_${it.value}" }
        return "$scope:$action:$paramString"
    }

    inline fun <reified T : Route> decodeRoute(raw: String): T {
        val params = parseCommonRoute(raw).params
        return decodeParams(params)
    }

    inline fun <reified T : Route> requireContext(): Pair<String, String> {
        val (scope, action) =
            getRouteContext(T::class)
                ?: error("Missing @RouteContext for ${T::class.simpleName}")
        return scope to action
    }

    fun <T : Route> getRouteContext(clazz: KClass<T>): Pair<String, String>? =
        clazz.annotations
            .filterIsInstance<RouteContext>()
            .firstOrNull()
            ?.let { it.scope to it.action }
}
