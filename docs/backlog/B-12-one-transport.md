---
id: B-12
title: "Name :ktg the transport and put :telegram into maintenance"
status: open
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
