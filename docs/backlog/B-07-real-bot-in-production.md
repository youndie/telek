---
id: B-07
title: "A bot that is not a sample: telek in production, as a native binary on :ktg"
status: open
priority: P1
size: L
stage: stage-2-dogfood
blocked_by: [B-05]
---

# B-07 — A bot that is not a sample: telek in production, as a native binary on :ktg

Everything telek knows about its own gaps comes from reading itself. The list of things deliberately
not done is ordered by how they feel from inside the library, and that order has never been tested
against anything that hurts. `:example` does not count — it shares the build, the version catalogue
and the source set, which are three of the things that break on publication, and its job is to
compile, not to run for a month.

- **The decision and its reason.** One real bot, deployed, doing something someone actually uses,
  built as a Kotlin/Native binary through `:ktg` — because that is the claim the README makes
  ("a bot can also ship as a native Linux binary") and a claim nobody has cashed is not a feature.
  A second consumer is the only instrument that finds what a library's own suite cannot; the
  sibling projects' record is that the first outside consumer finds several defects in a day.
- It also replaces the cat-fact example as the thing to point at. That is a side effect, not the
  reason — a showcase that exists only to be shown gets written to flatter the library.
- The rejected alternative is more sample modules. They are free, they compile, and they have
  already found everything they are going to find.
- Not covered: migrating any existing bot. This is a new one, small enough to throw away.

- AC: the bot runs as a `linuxX64` binary built from the **published** coordinate, in a container,
  for long enough to see a restart and a redeploy — not on a laptop.
- AC: whatever it finds is written into this backlog as items, including the ones that turn out to
  contradict the current ordering of the debts. That is the point of the exercise.
- AC: at least one of the deliberately-not-done items is either promoted or dropped as a result,
  with the bot as the evidence.
- Anchors: `README.md`, `ktg/`, `example/`.
