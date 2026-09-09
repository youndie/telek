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
