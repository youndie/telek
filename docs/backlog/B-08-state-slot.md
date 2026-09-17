---
id: B-08
title: "StateSlot: a bot with a user profile has nowhere to put it"
status: open
priority: P2
size: M
stage: stage-3-debts
---

# B-08 — StateSlot: a bot with a user profile has nowhere to put it

A conversation has exactly one `State`, and it is the wizard's (`UserStateStore`,
`core/src/commonMain/kotlin/io/github/youndie/telek/UserStateStore.kt`). Anything that outlives a
flow — a language, a timezone, a profile, "this user is an admin" — has no place in telek, so a bot
keeps it in its own store and then has two stores to keep consistent across the same transitions.
When the wizard's state reaches `FinalState` telek deletes its entry; a profile must not be deleted
with it, and today nothing distinguishes the two.

- **The decision and its reason.** Named slots under one key, rather than one state per key. The
  reason is that persistence is the module this falls on: `StateStorage<S>` saves one `S` per key,
  so a bot with a profile either serialises both into one class — coupling the profile's schema to
  the wizard's — or runs a second storage telek never sees, and the "exactly one update in flight
  per key" guarantee that `ChatWorkers` provides does not extend to it.
- This has been deferred before, and the reason it stayed deferred is worth keeping: nothing built
  since has needed it, and a slot API designed without a consumer is a guess. That is the same
  reason it should wait for [B-07](B-07-real-bot-in-production.md) rather than be designed now —
  a real bot with a profile is precisely the consumer this needs.
- The rejected alternative is a `State` that is a pair of flow-state and profile. It works and it
  is what a bot does today by hand; it is rejected as an API because `FinalState` then cannot mean
  what it means.
- Not covered: sharing a slot between conversations. That is a different problem and it is not a
  state machine's.

- AC: a bot stores a profile that survives a wizard reaching `FinalState`, without a second store.
- AC: a slot's serialised form does not change when an unrelated slot's type changes.
- Anchors: `core/src/commonMain/kotlin/io/github/youndie/telek/UserStateStore.kt`,
  `core/src/commonMain/kotlin/io/github/youndie/telek/StateStorage.kt`,
  `persistence/src/commonMain/kotlin/io/github/youndie/telek/persistence/PersistableUserStateStoreImpl.kt`.
