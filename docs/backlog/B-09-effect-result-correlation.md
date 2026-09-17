---
id: B-09
title: "onEffectResults cannot say which result came from which effect"
status: done
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

## Iteration 1 — 2026-09-17

Done. `EffectOutcome(effect, result)` is what `EffectExecutor.execute` returns and what
`onEffectResults` / `onEffectResult` receive.

- **The pairing is a wrapper, and that was forced rather than chosen.** The obvious shape — a field
  on `EffectResult` — cannot work: `EffectSuccess` is an `object`, one instance shared by every
  effect that succeeded, with nowhere to put "which effect". Pairing at the boundary also leaves
  every existing `EffectResult` implementation, the transports' included, untouched.
- **The acceptance criterion's second half is the whole argument, so it is a test of its own.**
  Inserting a third effect between the two sent messages changes every position and changes nothing
  about which result the dispatcher finds — the test says so, and its comment names what a
  positional reading would have taken instead.
- **Verified against a mutation:** pairing every outcome with `effects.first()` fails exactly three
  tests — both acceptance tests and the executor's own order test — and nothing else.
- The default's "pass only the last outcome to `onEffectResult`" behaviour is deliberately
  unchanged, as the item said: this was about the list being unusable, not about that choice.
- **Breaking, and named in `RELEASE_NOTES.md`:** a custom `EffectExecutor` changes its return type,
  and a dispatcher overriding either callback changes a parameter type and reads `.result`.
