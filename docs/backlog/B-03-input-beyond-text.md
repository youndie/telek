---
id: B-03
title: "Input is a text message or a callback, so no wizard can ask for a photo"
status: open
priority: P1
size: M
stage: stage-0-input-model
blocked_by: [B-01, B-12]
---

# B-03 — Input is a text message or a callback, so no wizard can ask for a photo

`Input` has exactly two implementations, `Message(chatId, text)` and `Callback(chatId, messageId,
data)` (`core/src/commonMain/kotlin/io/github/youndie/telek/Input.kt`). There is no photo, no
document, no contact, no location. "Send a photo of the document" and "share your phone number" are
not exotic wizard steps — they are most of what a wizard is for outside a menu, and today a
dispatcher cannot see that the user answered at all.

- **The decision and its reason.** Add the input types, and design the addition so that adding the
  *next* one is not a breaking change. `Input` is an ordinary public interface, not sealed, so a new
  subtype does not break a consumer's `when` — but `DefaultFindDispatcherStrategy` knows exactly
  `Message` and `Callback` (`core/src/commonMain/kotlin/io/github/youndie/telek/Telek.kt`), and an
  input it does not recognise routes by state alone. That is the part to get right once.
- Why it is blocked by [B-01](B-01-conversation-key.md) and not merely ordered after it: every new
  input type would be declared with a `chatId` that B-01 turns from the key into an address. Writing
  them first means writing them twice, and the second time inside a breaking release.
- The rejected alternative is an escape hatch — one `Raw`/`Unknown` input carrying the transport's
  own update object. Rejected as the default: it makes every bot import ktgbotapi types into its
  dispatchers, which is the one thing a transport-agnostic core is for. It may still be worth having
  *as well*, for what telek does not model.
- Not covered: sending media. That is an effect, and effects already have a place to grow.
- **Blocked by [B-12](B-12-one-transport.md), and that is now in the frontmatter rather than only
  here.** Every input type has to be adapted, tested and documented once per transport, so settling
  which transports get new work is the difference between paying that cost once and twice. The prose
  said "settle that first" from the start; the field did not, so the picking rule would have taken
  this item first and paid twice.

- AC: a wizard step asks for a photo, receives it, and can reach the file through telek's own types
  without naming a transport type in the dispatcher.
- AC: adding a further input type afterwards is additive — shown by adding one, not by asserting it.
- Anchors: `core/src/commonMain/kotlin/io/github/youndie/telek/Input.kt`,
  `core/src/commonMain/kotlin/io/github/youndie/telek/Telek.kt`,
  `ktg/src/commonMain/kotlin/io/github/youndie/telek/ktg/KtgInputs.kt`,
  `telegram/src/main/kotlin/io/github/youndie/telek/telegram/Connect.kt`.
