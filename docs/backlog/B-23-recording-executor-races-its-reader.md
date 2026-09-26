---
id: B-23
title: "RecordingEffectExecutor's journal is read and written from two threads without a lock"
status: done
priority: P2
size: XS
stage: stage-2-dogfood
---

# B-23 — `RecordingEffectExecutor`'s journal is read and written from two threads without a lock

`:testing`'s `RecordingEffectExecutor` kept its journal in a plain `mutableListOf`. `execute` appends
to it on whichever thread runs the chat worker; `effects` flattens it and `executed` hands out the
live list, on the test's own thread. A harness that waits for a bot to go quiet does exactly that —
it polls `effects.size` while effects are still being recorded — and the result is
`ConcurrentModificationException` out of `effects`.

Found by the one consumer, not by telek's own tests: the closed bot's 500-cycle memory run on
linuxX64 threw it a few seconds in, before producing a single number, while the same harness's menu
scenarios passed — they read the journal a handful of times, not in a loop. It is a
[B-21](B-21-native-binary-claim-uncashed.md) finding in that sense: the run that measures the native
binary could not start.

- **The journal is guarded by atomicfu's `SynchronizedObject`, and both properties return
  snapshots.** The same lock `:core` already uses in `ChatWorkers`, so no new dependency reaches a
  consumer. `execute` copies the batch it records, so a caller reusing its list cannot change the
  journal after the fact either.
- **`executed` is no longer a live view.** It never said it was one; a caller that kept the list and
  read it later now sees the moment it asked. Nothing in telek or its consumer does that.
- Rejected: a `Mutex`. The properties are plain getters, not `suspend`, and making them suspend would
  break every call site for a critical section that never suspends.

- AC: `RecordingEffectExecutorTest` — a writer on `Dispatchers.Default` records 2 000 batches while
  the test reads both properties in a loop — passes on jvm and linuxX64, and **fails with
  `ConcurrentModificationException` on linuxX64 against the previous implementation** (checked by
  swapping the old file back in: 8 runs of 8). Kept small on purpose — every read copies the whole
  journal, so the test is quadratic in its size, and 20 000 ran into `runTest`'s timeout on the CI
  runner.
- Anchors: `testing/src/commonMain/kotlin/io/github/youndie/telek/testing/RecordingEffectExecutor.kt`,
  `testing/src/commonTest/kotlin/io/github/youndie/telek/testing/RecordingEffectExecutorTest.kt`.
