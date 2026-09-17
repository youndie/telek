---
id: B-14
title: "Every diagnostic the library emits is off by default, because the logger is NoOp"
status: done
priority: P2
size: S
stage: stage-3-debts
---

# B-14 — Every diagnostic the library emits is off by default, because the logger is NoOp

`TelekLogger.NoOp` is the default for `Telek` and for `FileStateStorage`
(`core/src/commonMain/kotlin/io/github/youndie/telek/TelekLogger.kt`), so a bot that configures
nothing gets a library that reports nothing. Three things telek has decided are worth saying are
therefore said to no one unless the author already knew to ask: an input dropped because a
conversation's inbox was full, an effect that failed, and — since
[B-02](B-02-key-migration-on-disk.md) — a stored file that an upgrade has superseded.

Each of those is a moment where the bot's behaviour changes and nothing in the bot's own code
explains why. The drop is the sharpest: a user's message vanishes and the only record of it is a
warning nobody receives.

- **The decision and its reason.** Decide whether a library's default should be silence. The
  reason to treat it as one question rather than three is that the answer is the same for all of
  them and it is a stance, not a bug: silence-by-default is right for a library that might be
  embedded somewhere with its own logging, and wrong for one whose failure modes are invisible
  from the outside.
- The obvious alternative — a default that writes warnings and errors to stderr — is not
  obviously correct on Kotlin/Native, in a container, or under a bot that already routes logs
  somewhere. That is exactly why this is a decision and not a patch.
- A third option worth pricing: keep `NoOp` but make the constructors' KDoc say what goes
  unheard, so the choice is at least informed at the call site.
- Not covered: adding new diagnostics. This is about whether the existing ones are audible.

- AC: whichever is chosen, a bot that configures nothing either receives these three messages or
  has been told in the API's own documentation that it will not.
- Anchors: `core/src/commonMain/kotlin/io/github/youndie/telek/TelekLogger.kt`,
  `core/src/commonMain/kotlin/io/github/youndie/telek/Telek.kt`,
  `core/src/commonMain/kotlin/io/github/youndie/telek/ChatWorkers.kt`,
  `persistence/src/commonMain/kotlin/io/github/youndie/telek/persistence/FileStateStorage.kt`.

## Iteration 1 — 2026-09-17

Decided: **`NoOp` stays, and the cost is now stated where the choice is made.** A library that
writes to stderr has taken a decision belonging to the program embedding it, and the item itself
noted that stderr is not obviously right on Kotlin/Native or in a container. What was actually wrong
was not the default but that nothing said what it costs. The decision is also cheap to reverse — one
default parameter — which is what makes it safe to take rather than escalate.

- **The item undercounted: there are six diagnostics, not three.** Counted by grepping the call
  sites rather than by memory. `TelekLogger` now carries both, and splits them the way that
  matters: three are reported **nowhere else** (a dropped input, an `AsyncEffectHandler` that threw,
  a state file that could not be read), and three survive silence another way (a synchronous
  handler's failure reaches `TelekInterceptor.onError`; a failed save throws; the superseded-file
  warning concerns a key that is new anyway). The first table is the one worth reading.
- Every public constructor that takes a `logger` now says so at the call site, which is what the
  item's third option asked for.
- **Found and filed, not fixed here:** an `AsyncEffectHandler` that throws reaches **no**
  interceptor — `runAsync` catches, logs and returns `null`, and `null` there also means "nothing to
  report". So the two halves of one mechanism report failure to two different places and the async
  half reports it to the one that is off by default. That is a behaviour defect rather than an
  audibility one; it is [B-16](B-16-async-effect-failure-never-reaches-the-interceptor.md).
- **Beyond the acceptance criterion, deliberately:** the list is prose beside a growing set, so the
  seventh `logger.` call anybody adds would make it wrong with nothing saying so.
  `scripts/diagnostics_documented.py` counts the call sites against the number the documentation
  states and runs in CI. Checked by adding a seventh call and watching it fail with the file and
  line, then removing it. It counts; it cannot tell whether the descriptions are right, and its own
  docstring says so.
