---
id: B-07
title: "A bot that is not a sample: telek in production, as a native binary on :ktg"
status: done
priority: P1
size: L
stage: stage-2-dogfood
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

## Iteration 1 — 2026-09-17

**Closed on the half that was the point, with the other half split out rather than assumed.**

There is a real bot: closed-source, not in this repository, a production service with users, built
on `:ktg` and resolving telek from the published coordinate — not from a project dependency, not
from a shared catalogue, not from this build. It is the second implementation this item was asking
for, and it paid immediately.

- **It found what the suite could not, on the first day.** Migrating it across the conversation-key
  change surfaced a silent re-keying that neither telek's own tests nor the bot's several hundred
  notice: a bot that merely upgrades gets its key changed under it, stored state stops being found,
  and one that also feeds inputs through `Telek.onInput` directly ends up with one conversation
  split across two keys. That became [B-20](B-20-connect-rekeys-an-existing-bot-silently.md), and
  `connect()` now takes `keying` as a required argument — a compile error at the seam that decides
  it. That is this item's second and third criteria met in one finding: written into the backlog,
  and it changed a decision B-01 had already made.
- **A defect telek's own build cannot see, because telek's own build shares everything with itself.**
  The same reasoning produced `ci/consumer` (B-17), which then found two more — B-18 and B-19 — and
  those are permanent instrumentation rather than a one-off.
- **The first criterion is NOT met, and the difference is worth naming rather than rounding off.**
  It asks for a `linuxX64` binary running in a container. The bot has a native build and publishes a
  native image on every change — which is what keeps the shared code from quietly acquiring `java.*`,
  and that is real value — but the deployed workload is the JVM image. The switch between them is a
  manual flag, and it is off. Checked by looking at what is running, not at what is built: the
  published native image proves it links, not that it runs.

So the README's sentence — "a bot can also ship as a native Linux binary" — is still a claim nobody
has cashed. That is [B-21](B-21-native-binary-claim-uncashed.md), and it is small and specific,
which is a better shape than leaving this L-sized item open around it.
