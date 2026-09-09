package io.github.youndie.telek.router

import io.github.youndie.telek.State
import io.github.youndie.telek.StateDispatcher

public abstract class StateDispatcherWithRoutes<S : State> : StateDispatcher<S>() {
    protected abstract val routeRegistry: RouteRegistry

    override fun canHandleCallback(data: String): Boolean {
        if (super.canHandleCallback(data)) return true
        if (routeRegistry.canDecode(data)) return true
        return false
    }
}
