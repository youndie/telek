---
id: B-09
title: "onEffectResults cannot say which result came from which effect"
status: open
priority: P2
size: S/M
stage: stage-3-debts
---

# B-09 — onEffectResults cannot say which result came from which effect

A transition adds a list of effects; `onEffectResults(state, effectResults)` receives a list of
results (`core/src/commonMain/kotlin/io/github/youndie/telek/StateDispatcher.kt`). Nothing connects
the two lists — not an index, not an id, not the effect itself — and the default implementation
takes `effectResults.lastOrNull()` and drops the rest. A dispatcher that sends two messages and
needs the id of the *first* one cannot have it, and the shape of the API hides that: it looks like
a list, so it reads as though the correlation is there to be found.

- **The decision and its reason.** Carry the effect, or a handle to it, on its result. The reason is
  the failure mode rather than the missing feature: a positional assumption about these two lists
  is correct until an effect is added to the transition, and then it is silently wrong — the
  dispatcher edits the wrong message, and no test that asserts on a single effect can see it.
- The rejected alternative is to document the ordering guarantee and let callers index into the
  list. Rejected: it makes adding an effect to a transition a breaking change for that dispatcher,
  which is not a thing anyone will remember.
- Not covered: the default's "take the last one" behaviour on its own. That was a deliberate choice
  and it stays until something needs otherwise; this item is about the list not being usable at all
  when a dispatcher does look past the last element.

- AC: a dispatcher that adds two send-message effects can tell the two resulting message ids apart,
  and a test breaks if a third effect is inserted between them.
- Anchors: `core/src/commonMain/kotlin/io/github/youndie/telek/StateDispatcher.kt`,
  `core/src/commonMain/kotlin/io/github/youndie/telek/EffectResult.kt`,
  `core/src/commonMain/kotlin/io/github/youndie/telek/EffectExecutor.kt`.
