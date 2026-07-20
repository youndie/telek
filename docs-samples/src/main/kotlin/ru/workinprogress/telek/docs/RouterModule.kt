// Compiled copy of README.md's "Router module" section — keep both in sync.
package ru.workinprogress.telek.docs

import kotlinx.serialization.Serializable
import ru.workinprogress.telek.Callback
import ru.workinprogress.telek.Input
import ru.workinprogress.telek.router.Route
import ru.workinprogress.telek.router.RouteContext
import ru.workinprogress.telek.router.callback
import ru.workinprogress.telek.router.isRouteOf
import ru.workinprogress.telek.router.routes
import ru.workinprogress.telek.router.tryDecode
import ru.workinprogress.telek.telegram.sendMessage
import ru.workinprogress.telek.transition

@RouteContext(scope = "example", action = "select")
@Serializable
class ExampleRouteSelect(
    val number: Int,
) : Route

@RouteContext(scope = "example", action = "confirm")
@Serializable
class ExampleRouteConfirm : Route

@RouteContext(scope = "example", action = "cancel")
@Serializable
class ExampleRouteCancel : Route

private val registry =
    routes {
        register<ExampleRouteSelect>()
        register<ExampleRouteConfirm>()
        register<ExampleRouteCancel>()
    }

fun routerModuleSendSample(input: Input) =
    transition<ExampleState> {
        // Build inline keyboard with typed routes
        sendMessage(
            chatId = input.chatId,
            message = { row { text("Choose:") } },
            keyboard = {
                row {
                    // `callback(name, route)` comes from the router-telegram module
                    callback(name = "Confirm", route = ExampleRouteConfirm())
                    callback(name = "Cancel", route = ExampleRouteCancel())
                }
            },
        )
    }

// Handle callbacks in a dispatcher
fun routerModuleHandleSample(input: Input) {
    when (input) {
        is Callback -> {
            when {
                input.isRouteOf<ExampleRouteConfirm>(registry) -> { /* handle confirm */ }
                input.isRouteOf<ExampleRouteCancel>(registry) -> { /* handle cancel */ }
                else ->
                    input.tryDecode<ExampleRouteSelect>(registry)?.let { route ->
                        val n = route.number
                        n.let { /* handle selection of `n` */ }
                    }
            }
        }
        else -> { /* other inputs */ }
    }
}
