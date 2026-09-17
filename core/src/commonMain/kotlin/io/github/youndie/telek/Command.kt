package io.github.youndie.telek

/**
 * A command as Telegram actually delivers one: a name, optionally the bot it is addressed to, and
 * optionally an argument.
 *
 * `/start`, `/start@mybot`, `/start ABC-123` and `/start@mybot ABC-123` are all the `start` command.
 * The `@bot` suffix is not optional in a group — Telegram appends it whenever more than one bot can
 * see the message — and the argument is how a deep link or a shared identifier enters a bot at all.
 */
public data class Command(
    public val name: String,
    public val addressedTo: String? = null,
    public val argument: String? = null,
)

/**
 * Parses [Message.text] as a command, or returns `null` when it is not one.
 *
 * Splitting happens at the first whitespace rather than at a space, because a command pasted with a
 * newline after it is still that command — Telegram's own clients produce it.
 */
public fun Message.asCommand(): Command? {
    if (!text.startsWith("/")) return null

    val split = text.indexOfFirst { it.isWhitespace() }
    val head = if (split == -1) text else text.substring(0, split)
    val argument =
        if (split == -1) {
            null
        } else {
            text.substring(split).trim().takeIf { it.isNotEmpty() }
        }

    val body = head.removePrefix("/")
    val name = body.substringBefore('@')
    if (name.isEmpty()) return null

    return Command(
        name = name,
        addressedTo = body.substringAfter('@', missingDelimiterValue = "").takeIf { it.isNotEmpty() },
        argument = argument,
    )
}
