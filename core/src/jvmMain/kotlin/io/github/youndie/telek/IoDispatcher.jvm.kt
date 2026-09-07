package io.github.youndie.telek

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

actual val telekIoDispatcher: CoroutineDispatcher = Dispatchers.IO
