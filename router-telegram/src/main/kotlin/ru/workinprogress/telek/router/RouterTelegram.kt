package ru.workinprogress.telek.router

import ru.workinprogress.telek.telegram.RowBuilder

/**
 * The only bit of `:router` that actually needs `:telegram` — pulled into its own module so
 * `:router`'s core encode/decode logic can be used with a different transport without dragging
 * `:telegram` along.
 */
fun RowBuilder.callback(
    name: String,
    route: Route,
) {
    callback(name, route.encode())
}
