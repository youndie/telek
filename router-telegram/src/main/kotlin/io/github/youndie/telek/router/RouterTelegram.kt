package io.github.youndie.telek.router

import io.github.youndie.telek.telegram.RowBuilder

/**
 * The only bit of `:router` that actually needs `:telegram` — pulled into its own module so
 * `:router`'s core encode/decode logic can be used with a different transport without dragging
 * `:telegram` along.
 */
public fun RowBuilder.callback(
    name: String,
    route: Route,
) {
    callback(name, route.encode())
}
