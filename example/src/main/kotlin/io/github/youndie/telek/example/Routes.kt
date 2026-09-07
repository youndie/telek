package io.github.youndie.telek.example

import io.github.youndie.telek.router.Route
import io.github.youndie.telek.router.RouteContext
import kotlinx.serialization.Serializable

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
