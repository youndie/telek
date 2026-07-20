package ru.workinprogress.telek.router

import ru.workinprogress.telek.State
import ru.workinprogress.telek.StateDispatcher

abstract class StateDispatcherWithRoutes<S : State> : StateDispatcher<S>() {
    protected abstract val routeRegistry: RouteRegistry

    override fun canHandleCallback(data: String): Boolean {
        if (super.canHandleCallback(data)) return true
        if (routeRegistry.canDecode(data)) return true
        return false
    }
}
