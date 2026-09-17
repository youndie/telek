---
id: B-03
title: "Input is a text message or a callback, so no wizard can ask for a photo"
status: done
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

## Iteration 1 — 2026-09-17

Done. `Photo`, `Document`, `Contact` and `Location` added, each carrying a `messageId` and — for the
two that reference a file — a `FileRef` of `fileId`, `uniqueId` and `sizeBytes`. Adapters in `:ktg`
only; `:telegram` is in maintenance as of [B-12](B-12-one-transport.md), and this is the first item
that cashes that decision rather than paying for it twice.

- **Routing did not have to change, and that turned out to be the finding rather than an
  assumption.** Only a command and a callback carry routing information of their own, because only
  they can arrive with no state to belong to; everything else already fell through to "route by the
  conversation's current state", which is exactly what a wizard step wants. What the item called
  "the part to get right once" was therefore a matter of documenting and testing an existing
  property, not of writing a mechanism.
- **The additivity criterion is shown, not asserted, twice over.** A test declares an `Input`
  implementation *in the test file* and watches it reach a dispatcher with no line of `:core`
  knowing it exists. And the regenerated ABI dumps are **purely additive** — the diff removes
  nothing — which is the mechanical form of the same claim.
- **A claim was narrowed because it could not be tested.** The first draft's KDoc said `Photo.file`
  is "the largest size Telegram offered". `PhotoContent`'s collection is a value class over the size
  list and cannot be built from a test without reaching into ktgbotapi's internals, so that claim
  had no check behind it; it now says what is true and testable — the adapter carries
  `content.media`, the size the transport designates.
- **Verified against a mutation:** making the strategy refuse anything that is not a `Message` or a
  `Callback` fails exactly four tests — the engine's photo wizard, the three routing tests — and
  nothing else.
- `:docs-samples` caught its own bug on the first pass, which is what it is for: the README's new
  example needs `import io.github.youndie.telek.transition`, because inside a `StateDispatcher` the
  bare name resolves to the member `transition(state, input)` instead.
