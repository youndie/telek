---
id: B-08
title: "StateSlot: a bot with a user profile has nowhere to put it"
status: dropped
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
  a real bot with a profile is precisely the consumer this needs. **That is in the frontmatter now
  rather than only here**, because the picking rule reads the field and would otherwise have taken
  this item next and designed the API on a guess, which is the one thing its own text argues
  against.
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

## Dropped — 2026-09-17

**Dropped, and by the consumer this item said to wait for.** [B-07](B-07-real-bot-in-production.md)
was closed the same day, so the question this item deferred — what a real bot with a profile
actually does — could be asked instead of guessed. The answer is that it does neither of the two
things written above.

**The premise is wrong.** This item says a bot with a profile "either serialises both into one class
— coupling the profile's schema to the wizard's — or runs a second storage telek never sees". The
real one does a third thing, which is the option this item lists and rejects as an API: its own row
holds the flow state as **one field** beside the profile, and it implements `UserStateStore` to
project that field in and out. Telek sees a `State`; the bot sees its row.

It costs about forty lines, and the two hazards a reader would trip on are both in that adapter:
a `FinalState` arriving in `update`, and `clear`. The shipped stores delete the entry on both —
`DefaultUserStateStore` drops the map entry, `PersistableUserStateStoreImpl` deletes the file — and
a bot that copies that reading loses the profile. The real one gets it right, and it got it right by
reading the default implementation's source, because **nothing in the contract said so**. That was
the actual defect here, and it is a docstring rather than an API.

**So the fix is what shipped instead of a slot API:**

- `UserStateStore`'s KDoc now says what the store owns, that a `FinalState` and a `clear` mean *this
  flow is over* rather than *this person is gone*, and that a bot with data outliving a flow
  implements the interface rather than running a second store.
- `README.md` gains a short section with a working store, compiled in `:docs-samples` like every
  other block, so the pattern is checked rather than described.
- The persistence section's "the entry is automatically deleted" stops reading as the only
  possibility and points at the choice.

**Why an API is worse than the seam, now that there is evidence.** The thing the adapter buys is
that both halves are written inside the same `update` call, which `Telek` has already serialized per
conversation. A slot API would have to reproduce that, and a second store cannot give it at all —
which is the one real argument in this item, and it is already satisfied. What is left is forty
lines of adapter against a public interface that would grow a second concept, a second serialised
form, and a migration for anyone who had used the first one.

Reopen it if a second consumer writes the same adapter and gets it wrong in a way the docstring
does not prevent. One consumer writing it correctly is not a case for an API.
