package ru.workinprogress.telek.router

import ru.workinprogress.telek.State
import ru.workinprogress.telek.StateDispatcher

interface RouteAwareDispatcher {
    val routeRegistry: RouteRegistry
}

abstract class StateDispatcherRouteAware<S : State> :
    StateDispatcher<S>(),
    RouteAwareDispatcher {
    abstract override val routeRegistry: RouteRegistry

    override fun canHandleCallback(data: String): Boolean = routeRegistry.canDecode(data)
}
