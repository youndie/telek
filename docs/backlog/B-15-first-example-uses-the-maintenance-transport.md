---
id: B-15
title: "The first worked example is written against the transport in maintenance"
status: done
priority: P2
size: S
stage: stage-4-positioning
---

# B-15 — The first worked example is written against the transport in maintenance

[B-12](B-12-one-transport.md) made `:ktg` the transport and put `:telegram` into maintenance, and
`README.md`'s installation section says so. The first worked example, two screens further down, is
still written against `:telegram` — `io.github.youndie.telek.telegram.sendMessage`,
`…telegram.editMarkup` — under a heading that reads "Usage with Telegram bot" and an opening line
naming kotlin-telegram-bot.

So a reader is told which module to depend on and then shown, at length, code that uses the other
one. Whichever they copy, the README contradicted itself first.

- **The decision and its reason.** Rewrite the example against `:ktg`, and let the `:telegram`
  variant survive as the short "and the same on kotlin-telegram-bot" note rather than as the thing a
  newcomer reads first. The reason is not tidiness: the example is what people copy, and copying it
  today lands them on the module that does not get new input types and cannot be part of the native
  binary the same README advertises.
- The rejected alternative is to move the installation block's emphasis back. That undoes B-12 to
  make the README consistent, which is fixing the wrong half.
- Not covered: the section headings. "Using ktgbotapi instead" is a historical name and renaming it
  breaks every external link to the anchor — that was decided in B-12 and stands.

- AC: the first worked example imports `io.github.youndie.telek.ktg.*`, and `:docs-samples`
  compiles it against `:ktg`.
- AC: the test beside it ([B-10](B-10-readme-first-example.md)) still runs, since it is about the
  transition and not about the transport.
- Anchors: `README.md`, `docs-samples/src/main/kotlin/io/github/youndie/telek/docs/UsageWithTelegramBot.kt`.

## Iteration 1 — 2026-09-17

Done. The worked example and the wiring that runs it are on `:ktg`; kotlin-telegram-bot keeps a
section of its own, which is what maintenance means — a module that still compiles, still has a
sample, and is not where a newcomer starts.

- **The headings were "not covered" by this item and one of them had to move anyway.** "Using
  ktgbotapi instead" described the transport that is now the default, so leaving it would have been
  the same contradiction one section lower. It reads "The same on kotlin-telegram-bot" now, with an
  explicit `<a id="-using-ktgbotapi-instead">` above it so existing links still land there — which
  is the concern [B-12](B-12-one-transport.md) had when it declined to rename anything, answered
  rather than inherited.
- **A duplicate went with it.** `KtgExampleDispatcher` in `:docs-samples` was a copy of the worked
  example for the ktg section — and [B-10](B-10-readme-first-example.md) never reached it, so it
  still matched callbacks with `callback(text =, data =)` and a raw string. The example *is* the ktg
  one now, so the copy is gone rather than fixed twice.
- **Verified by mutation, and the first attempt measured the wrong gate.** Putting the example back
  on the `:telegram` DSL first failed `ktlintMainSourceSetCheck` on import ordering, which says
  nothing about transports. Running `:docs-samples:test` directly fails both README tests with
  `NoSuchElementException: List is empty` — the test filters for `:ktg`'s `SendMessageEffect` and
  finds none. That is the check that actually binds the sample to the transport.
