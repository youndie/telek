---
id: B-01
title: "The FSM key is a chatId, so a group chat has one state for everyone in it"
status: done
priority: P0
size: L
stage: stage-0-input-model
---

# B-01 — The FSM key is a chatId, so a group chat has one state for everyone in it

`Input` declares `val chatId: Long` and nothing else identifies who sent it
(`core/src/commonMain/kotlin/io/github/youndie/telek/Input.kt`). That `Long` is not one field: it is
the key type of the whole engine — `Telek.onInput`/`onEvent`/`applyReducer`, `ChatWorkers.submit`
and `launchAsync`, `UserStateStore.get`/`update`/`clear`, `StateStorage.save`/`load`/`delete`,
`InitialStateProvider.initialState`, `TelekInterceptor`, `TransitionGate`, `Event.chatId`. In a
group every member therefore shares one state, one actor and one inbox: two people running the same
wizard overwrite each other's answers, and the second one's reply advances the first one's flow.

- **The decision and its reason.** Make the key an abstract type the transport chooses — a chat for
  a private conversation, a chat plus a user for a group — rather than keep a raw `Long`. The reason
  is not groups on their own; it is that a wizard is the thing this library exists for, and a wizard
  keyed by a chat is wrong in every chat that has more than one person in it. A library cannot leave
  that to the caller: the caller cannot re-key `ChatWorkers` from outside.
- The rejected alternative is to declare telek a private-chat library and write it down as a
  non-goal (see [B-11](B-11-non-goals.md)). It is honest and it costs nothing today. It is rejected
  because the same README sells "interactive systems" and because the cost of reversing it grows
  with every consumer, while the cost of doing it now is one repository with no external users.
- **The seam already exists, and it is smaller than it looks.** `Telek.onInput(chatId, input)` takes
  the key as a *parameter*, separate from the `Input`; nothing in `:core` ever reads `input.chatId`
  (`ktg/src/commonMain/kotlin/io/github/youndie/telek/ktg/Connect.kt` passes both). So `chatId` on
  `Input` and on the effects is not the key at all — it is the Telegram *address*, the same value
  that reaches `ChatId.fromId(effect.chatId)` in the effect handlers. The two roles coincide in a
  private chat and diverge in a group. Splitting them is the work: the key becomes abstract, the
  address stays `Long`, and every effect and `EffectResult` is untouched.
- Not covered: the storage migration this forces — [B-02](B-02-key-migration-on-disk.md) — and the
  new input types — [B-03](B-03-input-beyond-text.md). Both wait on the shape decided here.

- AC: two members of the same group run the same wizard concurrently and neither sees the other's
  answers; a test asserts it rather than a screenshot.
- AC: a private-chat bot written against the current API compiles after a mechanical change at the
  wiring point only — not inside its dispatchers.
- AC: the diff of `core/api/core.klib.api` is the statement of what broke, and the release notes
  quote it.
- Anchors: `core/src/commonMain/kotlin/io/github/youndie/telek/Input.kt`,
  `core/src/commonMain/kotlin/io/github/youndie/telek/Telek.kt`,
  `core/src/commonMain/kotlin/io/github/youndie/telek/ChatWorkers.kt`,
  `core/src/commonMain/kotlin/io/github/youndie/telek/UserStateStore.kt`,
  `core/src/commonMain/kotlin/io/github/youndie/telek/StateStorage.kt`,
  `core/src/commonMain/kotlin/io/github/youndie/telek/InitialStateProvider.kt`,
  `core/src/commonMain/kotlin/io/github/youndie/telek/TransitionGate.kt`,
  `core/src/commonMain/kotlin/io/github/youndie/telek/Event.kt`,
  `ktg/src/commonMain/kotlin/io/github/youndie/telek/ktg/Connect.kt`,
  `telegram/src/main/kotlin/io/github/youndie/telek/telegram/Connect.kt`.

## Iteration 1 — 2026-09-17

Done. `ConversationKey(chatId, userId: Long?)` with `chat()` / `chatAndUser()`, and a `Keying`
enum the transports use to derive one; `Telek`, `ChatWorkers`, `UserStateStore`, `StateStorage`,
`InitialStateProvider`, `TelekInterceptor` and `TransitionGate.post` take it.

What the work found, beyond what the item assumed:

- **`Event.chatId` is an address too, and needed no change.** `Telek` routes an event by the key
  the async work was launched for, never by `event.chatId` — so `Event`, like `Input` and every
  effect, was already address-only. The item expected to touch it.
- **The chat-only key's on-disk name is byte-identical to the old one** (`storageId` is the bare
  `chatId`), which was not designed for and turns out to matter: state written by any earlier
  version is still found under `Keying.PerChat`. That narrows
  [B-02](B-02-key-migration-on-disk.md) from "every bot's directory stops matching" to "only the
  new per-user default starts empty", and it is the reason the release note can offer
  `Keying.PerChat` as a deliberate, working fallback.
- **`Keying.PerUserInChat` as the default is a decision this item made** and the item did not
  name. It is correct in a group and indistinguishable from the old behaviour in a private chat,
  where the only sender is the only member; the alternative — defaulting to the old shape and
  making every bot opt in — leaves the defect switched on for anyone who does not read the notes.
- **One test is weaker than it looks and it is not hidden:** `ConnectTest`'s group case asserts
  through the same `ConversationKey.chatAndUser` the production path calls, so a mutation of that
  constructor does not fail it. The engine, key and storage tests do fail on exactly that
  mutation, which is what carries the acceptance.
