---
id: B-14
title: "Every diagnostic the library emits is off by default, because the logger is NoOp"
status: open
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
