package io.github.youndie.telek

public enum class TelekLogLevel {
    DEBUG,
    WARN,
    ERROR,
}

/**
 * telek's own logging seam: [println]/`java.util.logging` are not acceptable in a library, since
 * the consumer can neither redirect nor silence them. Pass an implementation that forwards to
 * whatever the consumer already uses (slf4j, kotlin-logging, ...) — telek has no opinion on the
 * concrete backend and does not depend on one.
 *
 * **The default is [NoOp], so a bot that configures nothing hears none of the six things telek
 * has decided are worth saying.** That is a deliberate stance — a library that writes to stderr
 * has made a decision belonging to the program that embeds it — but it is only defensible if the
 * cost is stated, so here is the whole list and what is lost with it.
 *
 * Three are **only** here. Nothing else in the API reports them, and with [NoOp] they are gone:
 *
 * | What happened | Where | Looks like, unheard |
 * |---|---|---|
 * | An input was dropped because that conversation's inbox was full | [ChatWorkers] | A user's message vanished |
 * | An [AsyncEffectHandler] threw | [EffectExecutorImpl] | The effect produced no [Event] — indistinguishable from having nothing to say |
 * | A stored state file could not be read | `FileStateStorage.load` | `null` — indistinguishable from a first-time user |
 *
 * Three are also reachable another way, so silence costs detail rather than the fact:
 *
 * | What happened | Also reaches |
 * |---|---|
 * | A synchronous [EffectHandler] threw | [TelekInterceptor.onError], as an [EffectFailed] |
 * | A stored state file could not be written | `FileStateStorage.save` throws |
 * | A per-user key found the chat-keyed file it supersedes | Nothing — but the key is new, so nothing is lost either, only unexplained |
 *
 * The first table is the one to read before deciding [NoOp] is fine for your bot.
 */
public interface TelekLogger {
    public fun log(
        level: TelekLogLevel,
        message: String,
        error: Throwable? = null,
    )

    public fun debug(message: String): Unit = log(TelekLogLevel.DEBUG, message)

    public fun warn(
        message: String,
        error: Throwable? = null,
    ): Unit = log(TelekLogLevel.WARN, message, error)

    public fun error(
        message: String,
        error: Throwable? = null,
    ): Unit = log(TelekLogLevel.ERROR, message, error)

    public companion object {
        /** Discards everything. See [TelekLogger] for the six messages that go with it. */
        public val NoOp: TelekLogger =
            object : TelekLogger {
                override fun log(
                    level: TelekLogLevel,
                    message: String,
                    error: Throwable?,
                ) {
                }
            }
    }
}
