---
id: B-05
title: "Publish to Maven Central — after the breaking changes, not before"
status: open
priority: P0
size: S
stage: stage-1-release
blocked_by: [B-01, B-03]
---

# B-05 — Publish to Maven Central — after the breaking changes, not before

The coordinates moved to `io.github.youndie.telek` in 0.2.0 *for* Central, and the artefacts are
still only snapshots on a personal Reposilite: `README.md` tells a stranger to add
`https://reposilite.kotlin.website/snapshots` to their build before they can type a dependency.
Nobody outside this account will do that, so the library has no external consumers, and the whole
argument in [B-07](B-07-real-bot-in-production.md) about what a real consumer finds stays theoretical.

- **The decision and its reason: after the breaking work, not before.** A version on Central is
  permanent — it cannot be rewritten or withdrawn. Publishing the current API and then landing
  [B-01](B-01-conversation-key.md) means the first version a stranger can resolve is also the one
  the next release breaks, and `BREAKING CHANGE` is the first thing they read. A Reposilite snapshot
  costs nothing to get wrong; Central does not. This inverts the obvious order deliberately.
- **It is genuinely small, and that was checked rather than assumed.** The publishing path already
  exists for the whole portfolio: `youndie/sborka`'s `central.yaml` is a `workflow_dispatch` that
  takes a repository, a ref and a version, and the entire contract a library has to meet is
  `sborka.central=true` in its `gradle.properties` — five sibling repositories already carry it and
  telek does not. The upload leaves the bundle **staged**; releasing it is a click by a person,
  on purpose, because of the sentence above.
- The rejected alternative is JitPack, which needs no release process at all. Rejected: it builds
  from a tag on demand, and a Kotlin Multiplatform module resolved that way is a well-known source
  of variant problems that would arrive as bug reports about telek rather than about JitPack.
- Not covered: the release notes themselves — they are written already — and the tag,
  which is [B-06](B-06-github-release.md).

- AC: `sborka.central=true` is in `gradle.properties` and the dispatch reaches a staged bundle.
- AC: a scratch project with a **cold** Gradle cache and `mavenCentral()` as its only repository
  resolves `io.github.youndie.telek:core` and compiles against it, on the JVM and on `linuxX64` —
  verified by running it, not by the Central UI showing the files.
- AC: `README.md`'s installation section no longer names Reposilite.
- Anchors: `gradle.properties`, `README.md`,
  `.github/workflows/publish-telek-snapshot.yaml`.
