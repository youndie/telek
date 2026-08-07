package ru.workinprogress.telek.persistence

import okio.FileSystem

/**
 * The host file system — okio's `FileSystem.SYSTEM`.
 *
 * Same shape (and same reason) as [ru.workinprogress.telek.telekIoDispatcher]: okio declares
 * `FileSystem.SYSTEM` as an `expect` extension in its own `systemFileSystem` source set rather
 * than in `commonMain`, so common code can't reference it directly.
 */
expect val systemFileSystem: FileSystem
