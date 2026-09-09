package io.github.youndie.telek

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

public actual val telekIoDispatcher: CoroutineDispatcher = Dispatchers.IO
