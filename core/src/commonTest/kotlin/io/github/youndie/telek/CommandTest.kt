package io.github.youndie.telek

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CommandTest {
    private fun command(text: String) = Message(chatId = 1, text = text).asCommand()

    @Test
    fun `a bare command is a name and nothing else`() {
        assertEquals(Command(name = "start"), command("/start"))
    }

    @Test
    fun `a command addressed to a bot keeps the name and records the address`() {
        assertEquals(Command(name = "start", addressedTo = "mybot"), command("/start@mybot"))
    }

    @Test
    fun `a command with an argument keeps both`() {
        assertEquals(Command(name = "start", argument = "ABC-123"), command("/start ABC-123"))
    }

    @Test
    fun `a command both addressed and carrying an argument keeps all three`() {
        assertEquals(
            Command(name = "start", addressedTo = "mybot", argument = "ABC-123"),
            command("/start@mybot ABC-123"),
        )
    }

    @Test
    fun `the argument keeps its own spaces and loses only the surrounding ones`() {
        assertEquals("two words", command("/echo   two words  ")?.argument)
    }

    // Telegram's clients produce this when a command is pasted with a newline after it.
    @Test
    fun `a newline separates the command from its argument just as a space does`() {
        assertEquals(Command(name = "echo", argument = "line"), command("/echo\nline"))
    }

    @Test
    fun `ordinary text is not a command`() {
        assertNull(command("start"))
        assertNull(command("send /start please"))
    }

    @Test
    fun `a lone slash is not a command`() {
        assertNull(command("/"))
        assertNull(command("/ start"))
    }

    @Test
    fun `an address with no name is not a command`() {
        assertNull(command("/@mybot"))
    }
}
