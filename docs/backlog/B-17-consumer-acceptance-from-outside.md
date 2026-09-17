---
id: B-17
title: "Acceptance from outside: a consumer with a cold cache, against Reposilite"
status: wip
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
