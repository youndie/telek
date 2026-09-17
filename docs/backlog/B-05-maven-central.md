---
id: B-05
title: "Publish to Maven Central — after the breaking changes, not before"
status: question
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

## Iteration 1 — 2026-09-17

**Stops half-finished on purpose, and the half it stops at is a decision rather than a limit.**

Done: `sborka.central=true` is in `gradle.properties`, with the contract and the reason next to it.
That one line is the whole build-side change, and it is load-bearing — checked by control rather
than asserted: with the flag off, `:core:tasks --all` lists no `publishToMavenCentral` at all; with
it on, the task is there. The full build is green with the publish plugin applied, so the javadoc
jar and signing configuration do not break anything that was passing before.

Not done, and not to be done by the loop:

- **The dispatch.** `youndie/sborka`'s `central.yaml` takes a repository, a ref and a version, and
  it uploads with a signing key and a portal token that live in that repository. Running it is an
  action against a public registry; the standing permission covers merging this repository's own
  pull requests when they are green, and it does not stretch to that.
- **The release of the staged bundle.** Even the dispatch only stages. sborka's own comment says
  why the last step is a person's: a version on Central can never be rewritten or taken back.

Carried, and blocked on the release actually happening:

- **AC 2** — a scratch project resolving `io.github.youndie.telek:core` from `mavenCentral()` with a
  cold cache — cannot be run before there is something to resolve. It is the acceptance that
  matters most, because it is the only one that tests publication rather than configuration.
- **AC 3** — `README.md` no longer naming Reposilite — is deliberately NOT in this change. Landing
  it now would leave the README pointing a stranger at `mavenCentral()` for an artifact that is not
  there yet, which is worse than the honest snapshot instructions it has today. It goes in the same
  change as the verification above, once the release is real.

Next iteration resumes here and will find those two criteria open; it should not close the item on
the strength of the flag alone.

## The question — 2026-09-17

**What was found.** The build-side half is done and merged; everything that remains needs somebody
to act against a public registry, and a version on Maven Central can never be rewritten or taken
back. `wip` was the wrong status for that: in this backlog `wip` means an iteration is mid-flight,
and the loop reads `wip` with no branch as work that was abandoned. This is not abandoned — it is
waiting, and the thing it waits for is a person.

**The choices.**

1. **Dispatch and release by hand.** Run `youndie/sborka`'s `central.yaml` against `youndie/telek`
   at a chosen ref and version, look at the staged bundle, release it. Then this item's two
   remaining criteria become runnable and the loop can finish it.
2. **Let the loop dispatch, and keep the release.** The dispatch only stages, so it is recoverable
   — an unreleased bundle can be dropped. This needs saying explicitly, because it is an action
   against a registry with credentials that are not this repository's.
3. **Not yet.** Nothing here expires. The library goes on publishing snapshots to Reposilite, and
   the README goes on telling the truth about where the artifacts are.

**One fact found after this question was written, and it belongs to the answer.**
[B-19](B-19-telegram-needs-a-repository-nobody-mentions.md) established that `:telegram` depends on
kotlin-telegram-bot, which is **404 on Maven Central and 200 on JitPack**. So publishing `:telegram`
there would put a POM on Central whose dependency Central cannot serve, and every consumer of that
module would have to add JitPack. That is a decision rather than an accident — publish `:ktg` and
the multiplatform modules and leave `:telegram` on the snapshot repository, or publish it too and
say plainly in the README that it needs a second repository. `:ktg` is unaffected: ktgbotapi is on
Central.

**Who decides.** The repository owner. What the loop must not do either way is close this item on
the strength of `sborka.central=true`: configuration is not publication, and the criterion that
would prove publication — a cold-cache scratch project resolving from `mavenCentral()` — is exactly
the one that cannot run until the release is real.
