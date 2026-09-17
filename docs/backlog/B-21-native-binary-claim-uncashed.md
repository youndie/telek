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

## What could be measured without a deploy — 2026-09-17

One of the three numbers AC 2 asks for does not need anything to run, so it is here rather than
waiting: **image size**, read from the registry manifests rather than from a build log.

| build | layers | compressed |
|---|---|---|
| JVM | 7 | 101.9 MB |
| native | 3 | 44.4 MB |

**Read it as an order of magnitude and not as a measurement.** The two tags are not the same build:
the JVM one is tonight's, the native one is from August, because the native image is only published
on a manual dispatch. So this says "roughly half", not "2.3× exactly", and a controlled pair would
have to come from one commit. It is consistent with the 4.4× a sibling service measured, which is
the only reason it is worth writing down at all before the rest.

**The other two numbers cannot be taken from here, and the reason is not effort.** RSS against a
container limit and startup are properties of a bot that is *running*, and a bot only runs with a
real Telegram token. Measuring an instance that fails to authenticate would produce a number for an
idle process that never polls — the shape of a green run where nothing was exercised. So AC 1 and the
rest of AC 2 need somebody to throw the switch: the native image is built and pushed on dispatch,
the tag is bumped in the chart automatically, and `bot.native.enabled` is left to a person on
purpose.

What that person should watch for, so the run is not wasted: RSS at rest and under a burst of
updates against the container's limit; time from start to first successful long poll; whether the
file-backed state on the mounted volume survives the switch in both directions; and behaviour across
a restart and a redeploy rather than at t=0.
