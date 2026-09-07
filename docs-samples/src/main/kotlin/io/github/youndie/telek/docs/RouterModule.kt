// Compiled copy of README.md's "Router module" section — keep both in sync.
package io.github.youndie.telek.docs

import kotlinx.serialization.Serializable
import io.github.youndie.telek.Callback
import io.github.youndie.telek.Input
import io.github.youndie.telek.router.Route
import io.github.youndie.telek.router.RouteContext
import io.github.youndie.telek.router.callback
import io.github.youndie.telek.router.isRouteOf
import io.github.youndie.telek.router.routes
import io.github.youndie.telek.router.tryDecode
import io.github.youndie.telek.telegram.sendMessage
import io.github.youndie.telek.transition

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

                else -> {
                    input.tryDecode<ExampleRouteSelect>(registry)?.let { route ->
                        val n = route.number
                        n.let { /* handle selection of `n` */ }
                    }
                }
            }
        }

        else -> { /* other inputs */ }
    }
}
