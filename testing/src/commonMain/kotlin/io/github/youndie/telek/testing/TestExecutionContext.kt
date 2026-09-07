package io.github.youndie.telek.testing

import io.github.youndie.telek.ExecutionContext

/** A no-op [ExecutionContext] for tests that don't exercise transport-specific effect handlers. */
object TestExecutionContext : ExecutionContext
