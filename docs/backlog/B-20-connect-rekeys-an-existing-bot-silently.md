---
id: B-20
title: "connect()'s keying default silently re-keys a bot that upgrades"
status: done
priority: P1
size: S
stage: stage-0-input-model
---

# B-20 — `connect()`'s keying default silently re-keys a bot that upgrades

[B-01](B-01-conversation-key.md) made `Keying.PerUserInChat` the default, for a good reason: it is
correct in a group and indistinguishable from the old behaviour in a private chat. That reasoning is
about a **new** bot. For a bot upgrading from a version before the key existed, the same default
means the key changes under it without a line of its code changing.

What that costs, concretely: every conversation is now filed under a key nobody has written to, so
stored state is not found and everyone mid-flow starts again. A bot that also feeds some inputs
through `Telek.onInput` directly — which the adapters are public to allow — gets worse than that:
those inputs keep the old key while `connect()`'s use the new one, and the same conversation is
split across two keys, in the same process, permanently.

`RELEASE_NOTES.md` describes the re-keying. It does not help someone who upgrades a version without
reading them, and the failure has no symptom an author would connect to a default parameter.

- **The decision and its reason.** Consider making `keying` a **required** parameter of `connect()`
  rather than a defaulted one. A required parameter turns a silent behaviour change into a compile
  error at exactly the seam that decides it, for a bot that has one line to change and a real choice
  to make. The cost is that a new bot must also say what it wants — which is a sentence of typing
  against a failure mode with no symptom.
- The rejected alternative is a louder release note. The note is already there and already explicit;
  the problem is that nothing makes a reader arrive at it.
- A third option worth pricing: keep the default and have `connect()` log a warning the first time
  it derives a per-user key — but telek's logger is `NoOp` by default
  ([B-14](B-14-diagnostics-are-off-by-default.md)), so that is a warning to nobody.
- Not covered: changing the default back. `PerUserInChat` is right for new bots, which is the
  decision B-01 made and this item does not reopen.

- AC: a bot that upgrades across the key change cannot leave the keying unstated by accident —
  either it does not compile, or something it cannot miss says what changed.
- AC: whatever is chosen, `ci/consumer` exercises `connect()` rather than only `Telek.onInput`, so
  the seam that decides the key is covered by something.
- Anchors: `ktg/src/commonMain/kotlin/io/github/youndie/telek/ktg/Connect.kt`,
  `telegram/src/main/kotlin/io/github/youndie/telek/telegram/Connect.kt`, `RELEASE_NOTES.md`,
  `ci/consumer/`.

## Iteration 1 — 2026-09-17

Done, by the option the item put first: `keying` is a **required** parameter of `connect()` in both
`:ktg` and `:telegram`. An upgrading bot no longer compiles until it says which key it wants, which
is the only form of "you cannot miss this" available at a seam whose failure has no symptom.

- **The choice is a compile error at the exact line that makes it.** Not a release note a reader has
  to arrive at, not a warning to a `NoOp` logger. The bot that upgrades reads one message from the
  compiler and is standing in the file that decides the answer.
- **It costs a new bot one word.** That was the whole case against; priced against a failure mode
  where stored state stops being found and nothing says why, one word is not a price.
- **`keying` moved ahead of `answerCallbackQueries`** in `:ktg`. A required parameter behind a
  defaulted one forces every call site to name it, which reads worse than the thing it documents.
  Recorded in the ABI dumps: the `connect$default` synthetic overload is gone.
- **`ci/consumer` compiles `connect()`**, satisfying the second AC. It never ran a bot and still
  does not — it links the call, which is what makes the required parameter a real constraint from
  outside the repository rather than one the repository asserts about itself. Both transports are
  covered: `:ktg` in the consumer, `:telegram` by `:example` and `docs-samples`.
- **The default is not reopened.** `PerUserInChat` is still the right answer for a new bot; it is
  now the answer a new bot writes down instead of inheriting.
