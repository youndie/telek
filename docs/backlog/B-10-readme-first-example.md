---
id: B-10
title: "The first README example is stringly typed, in a type-safe toolkit"
status: open
priority: P1
size: S
stage: stage-4-positioning
---

# B-10 — The first README example is stringly typed, in a type-safe toolkit

The repository describes itself as a "Type-safe Telegram bot toolkit". The first worked example in
`README.md` routes a callback like this, at line 107:

```kotlin
if (input.data.contains("example_confirm")) {
```

A raw substring match on transport data, in the first thirty seconds a reader spends here. The
router — the module that exists precisely so nobody writes that — is introduced at line 402, four
hundred lines below, where a reader who already closed the tab will not reach it.

- **The decision and its reason.** The first example uses routes, and a test sits next to it. The
  reason is that the answer to "why telek and not the FSM ktgbotapi already has, or the KSP wizards
  in vendelieu/telegram-bot" is: a pure transition function, effects as data, and a test that needs
  no Telegram. The current first example demonstrates none of the three, and demonstrates the
  opposite of the first word in the description.
- A test beside the example is the load-bearing half. It is the claim that cannot be made by any of
  the alternatives, and it is currently shown nowhere near the top of the page.
- The rejected alternative is to move the router section up. It is cheaper and it does not work: the
  reader does not compare sections, they copy the first block that looks like their problem.
- Not covered: the rest of the README's ordering, and the positioning statement itself — that is
  [B-11](B-11-non-goals.md).
- `:docs-samples` compiles one file per README section, so whatever replaces this must compile
  there too, and that is a feature: the first example stops being able to rot.

- AC: the first worked example in `README.md` matches a callback by route, not by `contains`.
- AC: a test of that example's transition appears in the same section, and compiles in
  `:docs-samples`.
- Anchors: `README.md`, `docs-samples/`, `router/`, `router-ktg/`.
