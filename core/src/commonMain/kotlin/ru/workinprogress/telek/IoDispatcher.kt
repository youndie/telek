package ru.workinprogress.telek

import kotlinx.coroutines.CoroutineDispatcher

/**
 * The dispatcher telek runs blocking-capable work on — synchronous [EffectHandler]s (see
 * [EffectExecutorImpl]) and, in `:persistence`, file I/O.
 *
 * This exists because `Dispatchers.IO` lives in kotlinx-coroutines' `concurrent` source set (JVM +
 * Native), not `common`, so common code can't reference it directly. Every telek target has a real
 * one, so both actuals are just `Dispatchers.IO`; a hypothetical JS target would actual this to
 * `Dispatchers.Default` and nothing else would have to change.
 */
expect val telekIoDispatcher: CoroutineDispatcher
