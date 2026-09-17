---
id: B-12
title: "Name :ktg the transport and put :telegram into maintenance"
status: done
priority: P1
size: S
stage: stage-4-positioning
---

# B-12 — Name :ktg the transport and put :telegram into maintenance

There are two transports and the README presents them as a free choice — "same API shape, pick
whichever Telegram client you already use" — with `:telegram` listed first and `:ktg` commented out
underneath. They are not equivalent. `:telegram` is JVM-only, because kotlin-telegram-bot is, so
`:telegram` and `:router-telegram` are the two modules that cannot be part of the native binary the
README advertises. `:ktg` is multiplatform and is the one [B-07](B-07-real-bot-in-production.md)
will run in production.

- **The decision and its reason.** Declare `:ktg` the transport, keep `:telegram` published and
  working but stop growing it. The reason is arithmetic, not taste: every new input type in
  [B-03](B-03-input-beyond-text.md) has to be adapted, tested and documented once per transport, and
  that cost lands on every single item in the input model. Deciding after B-03 means paying it once
  for nothing.
- Maintenance means what it says: it still builds, its tests still run, it is still published, and
  it does not get the new input types unless somebody asks. That is a different statement from
  deprecation, and the README should make the difference plain rather than imply abandonment.
- The rejected alternative is to keep both first-class. It is the current state, and it is what
  makes B-03 twice the work; it would be worth paying if either transport had users, and neither
  has any outside this repository.
- Not covered: removing `:telegram`. Nothing is gained by deleting a module that costs nothing to
  keep compiling.

- AC: `README.md` installation shows `:ktg` as the default and says in one line why.
- AC: the non-goals section ([B-11](B-11-non-goals.md)) records that a third transport is not coming
  and that `:telegram` is in maintenance.
- Anchors: `README.md`, `ktg/`, `telegram/`, `router-telegram/`, `router-ktg/`.

## Iteration 1 — 2026-09-17

Done as a documentation change, because that is all this item ever was: nothing in the build or the
code changes, `telegram` and `router-telegram` go on building, testing and publishing exactly as
before. What changes is which transport a reader reaches for first.

- `README.md`'s installation block now lists `ktg` uncommented and `telegram` commented out, with
  the reason in one line: `ktg` is multiplatform, so it is the one a native binary can contain.
- "Maintenance" is spelled out rather than implied, because the word is usually read as
  abandonment: still built, still tested, still published, not deprecated, and **no new input
  types** unless somebody asks.
- The "Using ktgbotapi instead" heading is now flagged for what it is — a historical name, since
  `telegram` was here first — rather than renamed. Renaming it would break every external link to
  the anchor, and the section is accurate under its own title.

- **Second acceptance criterion is NOT ticked.** It asks the non-goals section to record that a
  third transport is not coming — and that section does not exist yet; writing it is
  [B-11](B-11-non-goals.md). Creating it here would be doing B-11's work under B-12's number. The
  criterion is carried over: B-11 has the sentence to add.
- **Ordering found and fixed:** [B-03](B-03-input-beyond-text.md)'s body said to settle this item
  first and its `blocked_by` did not say so, so the picking rule would have taken B-03 next and
  adapted every new input type twice. The field now says what the prose always said.
