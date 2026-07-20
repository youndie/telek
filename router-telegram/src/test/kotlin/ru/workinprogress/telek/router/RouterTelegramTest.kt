package ru.workinprogress.telek.router

import com.github.kotlintelegrambot.entities.keyboard.InlineKeyboardButton
import kotlinx.serialization.Serializable
import ru.workinprogress.telek.telegram.InlineKeyboardBuilder
import kotlin.test.Test
import kotlin.test.assertEquals

class RouterTelegramTest {
    @Serializable
    @RouteContext(scope = "ext", action = "one")
    data class ExtRouteOne(
        val id: String,
    ) : Route

    @Test
    fun `RowBuilder callback with a route encodes data via Route encode`() {
        val route = ExtRouteOne("xyz")

        val markup =
            InlineKeyboardBuilder()
                .apply {
                    row {
                        callback(name = "Push", route = route)
                    }
                }.build()

        val button = markup.inlineKeyboard.single().single() as InlineKeyboardButton.CallbackData
        assertEquals("Push", button.text)
        assertEquals(route.encode(), button.callbackData)
    }
}
