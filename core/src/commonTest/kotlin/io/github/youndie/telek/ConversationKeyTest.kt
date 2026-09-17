package io.github.youndie.telek

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class ConversationKeyTest {
    @Test
    fun `a chat key and a chat-and-user key for the same chat are different keys`() {
        assertNotEquals(ConversationKey.chat(1), ConversationKey.chatAndUser(1, 1))
    }

    @Test
    fun `two members of one chat are different keys`() {
        assertNotEquals(ConversationKey.chatAndUser(-100, 11), ConversationKey.chatAndUser(-100, 22))
    }

    @Test
    fun `the same member of two chats is two keys`() {
        assertNotEquals(ConversationKey.chatAndUser(-100, 11), ConversationKey.chatAndUser(-200, 11))
    }

    @Test
    fun `a chat key renders as the bare chatId - what earlier versions wrote to disk`() {
        assertEquals("1", ConversationKey.chat(1).storageId)
        assertEquals("-1001234567890", ConversationKey.chat(-1001234567890).storageId)
    }

    @Test
    fun `a chat-and-user key renders both numbers and a negative chatId stays readable`() {
        assertEquals("-100.11", ConversationKey.chatAndUser(-100, 11).storageId)
    }

    @Test
    fun `storageId separates keys that differ across the chat-user boundary`() {
        // "1.23" must not be reachable from any other pair — a storageId collision would make two
        // conversations share a file, which is the failure this whole item is about.
        val keys = listOf(ConversationKey.chat(1), ConversationKey.chatAndUser(1, 23), ConversationKey.chat(123))
        assertEquals(keys.size, keys.map { it.storageId }.toSet().size)
    }

    @Test
    fun `PerUserInChat falls back to the chat when nothing identifiable sent the update`() {
        assertEquals(ConversationKey.chat(7), Keying.PerUserInChat.key(chatId = 7, userId = null))
        assertEquals(ConversationKey.chatAndUser(7, 9), Keying.PerUserInChat.key(chatId = 7, userId = 9))
    }

    @Test
    fun `PerChat ignores the sender entirely`() {
        assertEquals(ConversationKey.chat(7), Keying.PerChat.key(chatId = 7, userId = 9))
    }
}
