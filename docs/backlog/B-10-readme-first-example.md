---
id: B-10
title: "The first README example is stringly typed, in a type-safe toolkit"
status: done
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

## Iteration 1 — 2026-09-17

Done. The first example's buttons are routes (`ExampleConfirm` / `ExampleCancel`), the branch that
handles them is `input.isRouteOf<ExampleConfirm>(exampleRoutes)` rather than a substring match, and
the reason is stated where a reader meets it: a route is a type, so the compiler keeps the button and
its branch in step — rename one and the other stops compiling.

- **The test beside it is not merely compiled, it runs.** `:docs-samples` had no test source set;
  it has one now, so `./gradlew build` executes the README's test rather than only type-checking it.
  That is a stronger promise than the acceptance criterion asked for, and it is the cheaper one to
  keep honest — a sample that compiles can still assert nothing.
- The test touches no bot, no network and no coroutine, because that is the claim the example
  exists to make and the one neither ktgbotapi's FSM nor a KSP wizard generator makes for you.
- **The first draft of that test was weak and it was caught by asking what a mutation would do to
  it.** It asserted only `newState == Done` — and `Done` is the state for *both* buttons, so it
  would have passed with the two routes swapped, which is precisely the defect this item is about.
  It now asserts the reply as well, and swapping the routes in the dispatcher fails both cases.
- **Found, not fixed here:** the example is still written against `:telegram`, which
  [B-12](B-12-one-transport.md) put into maintenance two screens earlier in the same file. A reader
  is told to depend on `:ktg` and then shown a page of `:telegram` code. That is a defect B-12
  introduced and this item is not scoped to it, so it is filed as
  [B-15](B-15-first-example-uses-the-maintenance-transport.md) rather than folded in.
