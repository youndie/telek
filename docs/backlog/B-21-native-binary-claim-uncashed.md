---
id: B-21
title: "The README's native-binary claim has never been run"
status: open
priority: P2
size: M
stage: stage-2-dogfood
---

# B-21 — The README's native-binary claim has never been run

`README.md` says a bot "can also ship as a native Linux binary", and the multiplatform half of the
library exists to make that true: `:core`, `:ktg`, `:router`, `:router-ktg`, `:persistence` and
`:testing` publish for `linuxX64` and `linuxArm64`, and `ci/consumer` links a native binary against
the published coordinates on every CI run.

What none of that shows is a bot **running** that way. Linking proves the symbols resolve; it says
nothing about what happens over days — the allocator's RSS against a container limit, a TLS client
that has to be `curl` because CIO on Kotlin/Native has no TLS, long polling across a restart, a
file-backed state store on a mounted volume. Those are where a native service actually costs
something, and they are invisible to a build.

[B-07](B-07-real-bot-in-production.md) got telek a real consumer and that consumer found real
defects; its native image is built and published on every change, which keeps the shared code from
acquiring `java.*` unnoticed. But the deployed workload is the JVM one, behind a manual switch that
has not been thrown.

- **The decision and its reason.** Cash the claim or stop making it. A capability that is compiled
  but never run is the same shape as a deployed surface nobody calls: it looks like a feature and
  reports nothing when it stops working.
- The rejected alternative is a longer `ci/consumer` — running the linked binary in CI for a minute.
  That is worth having and is not this: a minute finds a startup failure, not a leak, and CI has no
  container limit worth measuring against.
- Not covered: making the native build the default anywhere. The point is one run with somebody
  watching, not a migration.

- AC: a bot built as a `linuxX64` binary from the **published** coordinates runs in a container with
  a memory limit, through at least one restart and one redeploy.
- AC: what it costs is written down as numbers — RSS against the limit, startup, image size — beside
  the same numbers for the JVM build, so the README's sentence can be read by someone deciding.
- AC: whatever it finds is filed here, including anything that contradicts the current ordering.
- Anchors: `README.md`, `ktg/`, `ci/consumer/`.
