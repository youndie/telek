package ru.workinprogress.telek.testing

import ru.workinprogress.telek.ExecutionContext

/** A no-op [ExecutionContext] for tests that don't exercise transport-specific effect handlers. */
object TestExecutionContext : ExecutionContext
