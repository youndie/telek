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

- **The journal is an immutable list in an `AtomicReference`, swapped whole on every write.** Both
  properties read a snapshot; `execute` records a copy of the batch, so a caller reusing its list
  cannot change the journal after the fact either. No lock: a reader polling in a loop never makes
  the recording thread wait.
- **Rejected first, after it shipped to CI: atomicfu's `SynchronizedObject`**, the lock `:core` uses
  in `ChatWorkers`. It passed everywhere it could be watched — 60 runs of 60 on the build box pinned
  to 1, 2, 4 and 20 cores — and hung twice on GitHub's runner (`UncompletedCoroutinesError`, the
  test never finished). The cause was **not** found; the working guess is a reader re-taking an
  unfair lock in a tight loop. Copy-on-write removes the question instead of answering it. Whether
  the same stall can reach `ChatWorkers` is open and not investigated here.
- Rejected: a `Mutex`. The properties are plain getters, not `suspend`, and making them suspend would
  break every call site.
- **`executed` is no longer a live view.** It never said it was one; a caller that kept the list and
  read it later now sees the moment it asked. Nothing in telek or its consumer does that.

- AC: `RecordingEffectExecutorTest` — a writer on `Dispatchers.Default` records 2 000 batches while
  the test reads both properties in a loop, bounded at 20 000 reads so a stall cannot become a test
  that never ends — passes on jvm and linuxX64. **Against the previous implementation it fails with
  `ConcurrentModificationException` on linuxX64 10 runs of 10** (checked by swapping the old file
  back in). On the JVM it does not catch the race at this size; it did at 200 000 batches, which
  every read copying the whole journal makes too slow for `runTest`'s timeout.
- Anchors: `testing/src/commonMain/kotlin/io/github/youndie/telek/testing/RecordingEffectExecutor.kt`,
  `testing/src/commonTest/kotlin/io/github/youndie/telek/testing/RecordingEffectExecutorTest.kt`.
