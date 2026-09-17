---
id: B-17
title: "Acceptance from outside: a consumer with a cold cache, against Reposilite"
status: done
priority: P1
size: M
stage: stage-1-release
---

# B-17 — Acceptance from outside: a consumer with a cold cache, against Reposilite

Everything telek knows about itself comes from its own build. `:example` and `:docs-samples` share
the build, the version catalogue and the source set — and those are three of the things publication
breaks, so neither can notice any of them. The library has been published as snapshots all day and
**nobody has ever resolved one**.

This is [B-05](B-05-maven-central.md)'s second acceptance criterion — a project with a cold cache
resolving telek and compiling against it — aimed at Reposilite instead of Maven Central, because
Central is deferred and this is the half that does not have to wait for it. It is also the half that
matters: it tests publication rather than configuration, and it is cheaper to discover a broken
publication on a snapshot than on a version that can never be withdrawn.

- **The decision and its reason.** A standalone Gradle project under `ci/consumer/`, sharing nothing
  with this build: not the version catalogue, not a source set, not a project dependency. What it
  knows is what a stranger knows — a coordinate and a repository URL. `mavenLocal` is excluded on
  purpose: `~/.m2` is shared with every build on this machine, so resolving from it would prove the
  machine rather than the publication.
- **Cold cache, and that is not decoration.** A warm `~/.gradle` can serve an artefact that was
  never published, or serve a stale one under a version that has moved. The run uses a
  `GRADLE_USER_HOME` of its own, created for the run.
- **Linked, not merely compiled**, on `linuxX64`. A klib that compiles and cannot be linked is a
  library nobody can ship, and compiling is where a consumer usually stops looking.
- The rejected alternative is another module inside this repository. Free, compiles, and has already
  found everything it is going to find.
- Not covered: Maven Central. When B-05's question is answered, the same consumer is aimed at it by
  changing one property.

- AC: a project outside this build resolves every published module from Reposilite with a cache
  created for the run, and compiles against the API as it stands today — the `ConversationKey`,
  the input types, `asCommand`, `EffectOutcome`.
- AC: the `linuxX64` binary is **linked**, not just compiled.
- AC: whatever it finds is filed as items rather than fixed quietly in the consumer.
- Anchors: `ci/consumer/`, `docs/backlog/B-05-maven-central.md`.

## Iteration 1 — 2026-09-17

Done, against `0.3.0.64` — the snapshot published from main after #39, so every change of the day is
in it. `ci/consumer/` resolves every published module with a `GRADLE_USER_HOME` created for the run,
compiles on jvm and linuxX64, **links** the native binary, and both binaries were run: each prints
its line and exits 0, so the `check(...)` assertions inside `main` — `ConversationKey`, `Keying`,
`asCommand`, `Photo`/`FileRef`, `EffectOutcome`, `stateStorageOf` — all hold through the published
artefacts rather than through this build.

**Three runs, three findings, and only one of them was telek's fault in the way I first guessed.**

1. **[B-18](B-18-published-api-is-hidden-behind-implementation.md)** — the consumer could not resolve
   `kotlinx.serialization.Serializable` while writing the annotations `:router` and `:persistence`
   *require* of it. Every module declares its dependencies as `implementation`, so most publish an
   `apiElements` of nothing but `kotlin-stdlib`. `core` is the sharpest case: `Telek`'s constructor
   takes a `CoroutineScope` that a consumer cannot name. `testing` does it correctly, which is what
   says this is an oversight and not a policy.
2. **[B-19](B-19-telegram-needs-a-repository-nobody-mentions.md)** — `:telegram` depends on
   kotlin-telegram-bot, which is 404 on Maven Central and 200 on JitPack. telek's own build adds
   JitPack; the README does not. Following the installation section exactly makes `:telegram`
   unresolvable, with an error naming kotlin-telegram-bot rather than telek.
3. **Not a defect, and checked before filing one:** the release link died with
   `OutOfMemoryError: Java heap space`. That is Kotlin/Native's appetite, not telek's — telek's own
   `gradle.properties` carries the same `-Xmx4g` for the same reason. Fixed in the consumer, with
   the reason written next to it, and no item filed.

**A false alarm I nearly filed.** Reading the root `router` module's metadata showed no
serialization in *any* jvm variant, including runtime — which would have been a
`NoClassDefFoundError` waiting for every consumer. It is not: for a KMP library the root module's
variants delegate through `available-at`, and the real dependencies are in `router-jvm`, whose
`jvmRuntimeElements` carries the full set. The runtime is fine; the cost is entirely at compile
time. Checked before writing it down.

**One number, noted rather than fixed:** the README badge says Kotlin 2.4.10; the published module
metadata says the artefacts were compiled with 2.4.20.
