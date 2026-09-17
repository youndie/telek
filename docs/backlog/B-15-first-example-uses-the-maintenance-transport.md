---
id: B-15
title: "The first worked example is written against the transport in maintenance"
status: open
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
