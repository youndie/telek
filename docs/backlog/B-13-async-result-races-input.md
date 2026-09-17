---
id: B-13
title: "Auto-cancelling an async result when new input arrives"
status: dropped
priority: P3
size: XS
stage: stage-3-debts
---

# B-13 — Auto-cancelling an async result when new input arrives

An `AsyncEffectHandler` can be in flight when the user sends something new. The `Event` it
eventually produces is routed back through the same per-chat worker, so it cannot interleave with
the input — ordering is not the question — but it arrives at a state that may no longer be the one
that started the work. The recurring proposal is that telek should cancel or discard it implicitly.

**Dropped, and this file exists so the proposal is answered in ten seconds the next time it comes
up.**

- **The reason.** Guarding on state in the transition is the correct answer, not a workaround: the
  handler's result is only stale relative to a state the dispatcher itself defines, and only the
  dispatcher knows whether a late result is worthless or worth applying anyway. An engine that
  discarded it would be guessing, and would do so invisibly.
- **Opt-in already exists** for the case where cancellation is right: `Debounced` gives an effect a
  `debounceKey`, and `ChatWorkers.launchAsync` cancel-and-replaces the previous in-flight job for
  that key in that chat (`core/src/commonMain/kotlin/io/github/youndie/telek/Debounced.kt`). Not
  implementing it means "run every dispatch independently", which is the right default because
  implicit auto-cancel-by-effect-class is surprising in exactly the cases where it matters.
- What would reopen this: [B-07](B-07-real-bot-in-production.md) finding that the state guard is
  written identically in every dispatcher of a real bot. A guard repeated everywhere is a missing
  feature; one written in three places is not.
- What is *not* dropped: documenting it. The race is real and a reader should meet it in the README
  next to `Debounced`, not discover it.

- Anchors: `core/src/commonMain/kotlin/io/github/youndie/telek/Debounced.kt`,
  `core/src/commonMain/kotlin/io/github/youndie/telek/ChatWorkers.kt`,
  `core/src/commonMain/kotlin/io/github/youndie/telek/AsyncEffectHandler.kt`, `README.md`.
