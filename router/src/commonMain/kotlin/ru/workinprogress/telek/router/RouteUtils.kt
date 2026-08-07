package ru.workinprogress.telek.router

import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.properties.Properties
import kotlinx.serialization.properties.encodeToStringMap
import kotlinx.serialization.serializer
import kotlin.reflect.KClass

@OptIn(ExperimentalSerializationApi::class, InternalSerializationApi::class)
object RouteUtils {
    private val serializerCacheLock = SynchronizedObject()
    private val serializerCache = mutableMapOf<KClass<*>, KSerializer<Any>>()

    fun encodeRouteDynamic(route: Route): String {
        val serializer =
            synchronized(serializerCacheLock) {
                serializerCache.getOrPut(route::class) {
                    @Suppress("UNCHECKED_CAST")
                    route::class.serializer() as KSerializer<Any>
                }
            }

        val (scope, action) =
            getRouteContext(serializer.descriptor)
                ?: error("Missing @RouteContext for ${route::class.simpleName}")

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
            getRouteContext(serializer<T>().descriptor)
                ?: error("Missing @RouteContext for ${T::class.simpleName}")
        return scope to action
    }

    /**
     * Reads [RouteContext] off a route's generated [SerialDescriptor] rather than off its [KClass].
     *
     * `KClass.annotations` is JVM-only — it needs full `kotlin-reflect`, which doesn't exist on
     * Kotlin/Native. `@RouteContext` is a [kotlinx.serialization.SerialInfo] annotation instead, so
     * the serialization compiler plugin bakes it into the descriptor at compile time and it can be
     * read identically on every target, with no reflection and no `kotlin("reflect")` dependency.
     * The one requirement this adds is that every [Route] must be `@Serializable` — including ones
     * with no properties at all.
     */
    fun getRouteContext(descriptor: SerialDescriptor): Pair<String, String>? =
        descriptor.annotations
            .filterIsInstance<RouteContext>()
            .firstOrNull()
            ?.let { it.scope to it.action }
}
