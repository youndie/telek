package ru.workinprogress.telek

enum class TelekLogLevel {
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
interface TelekLogger {
    fun log(
        level: TelekLogLevel,
        message: String,
        error: Throwable? = null,
    )

    fun debug(message: String) = log(TelekLogLevel.DEBUG, message)

    fun warn(
        message: String,
        error: Throwable? = null,
    ) = log(TelekLogLevel.WARN, message, error)

    fun error(
        message: String,
        error: Throwable? = null,
    ) = log(TelekLogLevel.ERROR, message, error)

    companion object {
        val NoOp: TelekLogger =
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
