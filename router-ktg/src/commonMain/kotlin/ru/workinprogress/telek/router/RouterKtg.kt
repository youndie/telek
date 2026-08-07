package ru.workinprogress.telek.router

import ru.workinprogress.telek.ktg.RowBuilder

/**
 * `:router-telegram`'s counterpart for the ktgbotapi transport — the one bit of `:router` that
 * needs to know about a transport at all, kept out of `:router` itself so its encode/decode logic
 * stays transport-agnostic.
 */
fun RowBuilder.callback(
    name: String,
    route: Route,
) {
    callback(name, route.encode())
}
