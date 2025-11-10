package ru.workinprogress.telek.example

import kotlinx.serialization.Serializable
import ru.workinprogress.telek.router.Route
import ru.workinprogress.telek.router.RouteContext

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
