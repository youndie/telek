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

## Half the question, answered — 2026-09-17

**`:telegram` goes to Central with everything else, and the README says why the second repository
is there.** This settles the fact B-19 carried in; the other half — who dispatches and who releases
— is untouched and still the owner's.

- **The choice was never between two repositories and one.** A consumer of `:telegram` needs
  JitPack either way: kotlin-telegram-bot is 404 on Central and 200 on JitPack, and that is true of
  the module, not of where telek is served from. Publishing `:telegram` to Central inherits that
  requirement; it does not create it.
- **The alternative is worse for exactly the consumer it was meant to protect.** "`:ktg` on Central,
  `:telegram` on snapshots" leaves a `:telegram` user adding JitPack *and* a personal Reposilite,
  and the Reposilite is the bigger ask by far — it is one account's server, which is the sentence
  that opens this item.
- **It is not on offer anyway, and that was checked rather than assumed.** `sborka.central` is read
  with `providers.gradleProperty` in the shared `publish` convention, so it is one flag for the
  whole repository. The only module-level control is applying `io.github.youndie.sborka.publish` or
  not — which is how `docs-samples` stays unpublished — and dropping it from `:telegram` would take
  that module off the **snapshot** repository too, where its current consumers resolve it. So the
  real options are all modules or none.
- **The instructions already exist and are already checked.** [B-19](B-19-telegram-needs-a-repository-nobody-mentions.md)
  put the JitPack line in `README.md`'s installation block, scoped to
  `io.github.kotlin-telegram-bot.*`, saying what it is for, what the failure looks like without it,
  and that `:ktg` needs none of it. `ci/consumer` copies that block, so the two cannot drift
  silently.
- **`:ktg` is where a reader is pointed, and it costs nothing extra.** ktgbotapi is on Central.
  `:telegram` is in maintenance; documenting one extra repository for it is cheaper than vendoring
  kotlin-telegram-bot, and much cheaper than leaving the module unresolvable.

**What this predicts, and where it would be caught.** A POM on Central naming a dependency Central
cannot serve is unusual but not forbidden — Central validates the bundle, not the resolvability of
its transitive graph. If the upload disagrees, the staged bundle is where that shows up, before
anything is permanent, and the fallback is to drop `sborka.publish` from `:telegram` and
`:router-telegram` and say in the README that those two stay on snapshots. Named here so whoever
dispatches recognises it instead of rediscovering it.

So AC 3 — `README.md` no longer naming Reposilite — now means: Central for the coordinates, JitPack
kept and kept explained. Not Reposilite.

## Deferred — 2026-09-17

**Not yet, by the owner's decision.** Choice 3 of the three above. Nothing here expires: snapshots
go on publishing to Reposilite (`0.4.0.71` is there), and `README.md` goes on saying truthfully
where the artifacts are. The asymmetry is the whole argument — a version on Central cannot be
withdrawn, a postponement can be ended any morning.

What keeps accruing while it waits is not a deadline but the thing this item opens with: the
library has no external consumers, so [B-07](B-07-real-bot-in-production.md)'s argument about what
a real consumer finds stays theoretical. That is a cost, and it is not an urgent one.

**The `:telegram` half is reopened, and the section above it is wrong in its emphasis.** It answered
"will Central accept it", and checking says probably yes: the
[publishing requirements](https://central.sonatype.org/publish/requirements/) are javadoc and
sources, checksums, PGP signatures and POM metadata; of dependencies they say only "strongly
recommend you include the correct dependencies", and they discourage `<repositories>` in a POM,
which Gradle does not emit anyway. No documented check that a dependency resolves from Central.
Re-verified alongside it: kotlin-telegram-bot is on Central under no coordinate at all — a search
for it returns nothing.

But acceptance was the wrong question. **A library on Central that cannot be built without adding a
third-party repository is bad citizenship, and in places where dependencies outside Central are
disallowed by policy it is not usable at all.** That argument does not care what the validator does.

So there is a third option, dismissed too quickly above: **drop `io.github.youndie.sborka.publish`
from `:telegram` and `:router-telegram`.** Then everything published resolves from Central alone —
`:core`, `:ktg`, `:router`, `:router-ktg`, `:persistence`, `:testing` — and those two stop being
published anywhere, snapshots included. The cost is not compatibility, because nothing outside this
account consumes them; the cost is that it is a decision about the module's future. `:ktg` is the
recommended transport and `:telegram` is in maintenance, so the decision is smaller than it sounds,
but it is not this item's to make.

One thing that is settled either way: if `:telegram` is published, the README's JitPack block stays
and stays explained. That part of the earlier section stands.
