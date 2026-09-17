---
id: B-20
title: "connect()'s keying default silently re-keys a bot that upgrades"
status: open
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
