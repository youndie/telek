package ru.workinprogress.telek.router

import dev.inmo.tgbotapi.types.buttons.InlineKeyboardButtons.CallbackDataInlineKeyboardButton
import kotlinx.serialization.Serializable
import ru.workinprogress.telek.ktg.InlineKeyboardBuilder
import kotlin.test.Test
import kotlin.test.assertEquals

class RouterKtgTest {
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

        val button = markup.keyboard.single().single() as CallbackDataInlineKeyboardButton
        assertEquals("Push", button.text)
        assertEquals(route.encode(), button.callbackData)
    }
}
