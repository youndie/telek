# telek — what differs here

The portfolio's shared rules live in the global `CLAUDE.md` and in the skill pack. This file holds
only what is specific to this repository: names, commands, and the guards that fail if you skip them.

## Coordinates and modules

Group `io.github.youndie.telek`, packages `io.github.youndie.telek.*`. Both moved in 0.2.0 — a
reference to `ru.workinprogress.telek` anywhere is stale, not a second namespace.

Published: `:core`, `:ktg`, `:telegram`, `:router`, `:router-ktg`, `:router-telegram`,
`:persistence`, `:testing`. Not published, and each is load-bearing anyway: `:example`,
`:docs-samples` (one compiled file per README code block — a sample that stops compiling fails the
build), and `ci/consumer`.

**Two transports, and they are not equals.** `:ktg` on ktgbotapi is *the* transport: multiplatform,
its dependency is on Maven Central, new work goes there. `:telegram` on kotlin-telegram-bot is in
maintenance, JVM-only, and its dependency exists **only on JitPack** — which is why the README's
installation block carries a group-filtered JitPack repository and an explanation of the error you
get without it.

## Where things run

Builds and tests go to the Linux box: `~/.claude/bin/wsl-run ./gradlew build`. Anything that
rewrites files in the working tree runs locally instead — `LOCAL=1 ./gradlew ktlintFormat`,
`LOCAL=1 ./gradlew updateKotlinAbi` — because the Linux side is a replica and edits made there reach
neither git nor the mac.

`:telegram:checkKotlinAbi` failing after an API change is the expected signal, not a problem: run
`updateKotlinAbi` locally, read the dump diff, and let it be part of the change.

## `ci/consumer` — the thing that checks publication

A separate Gradle build, deliberately outside `settings.gradle.kts`: no shared catalogue, no source
set, no project dependency. It knows a coordinate and a repository URL, which is what a stranger
knows. It has already found two publication defects invisible from inside the repository.

To point it at a local publication:

```bash
~/.claude/bin/wsl-run './gradlew publishToMavenLocal -PVERSION=<v>'
cd ci/consumer && ~/.claude/bin/wsl-run './gradlew build --no-build-cache -Ptelek.version=<v> -Ptelek.repo=file:///home/youndie/.m2/repository'
```

It refuses `mavenLocal()` on purpose — resolving from `~/.m2` implicitly would prove the machine
rather than the publication. The explicit `-Ptelek.repo` override is the supported way in.

Its Kotlin plugin pin is not decoration: Kotlin metadata is one-directional, so if the pin drifts
from the compiler telek was built with, this build fails. That makes it the enforced copy of that
version, and the README badge is checked against it.

## Backlog

docs-bootstrap format: one file per item in `docs/backlog/B-NN-<slug>.md`, index generated into
`backlog.md` (lowercase — it matches the other repositories, and macOS will not show you the
difference). Regenerate with `python3 scripts/backlog_index.py` in the same change as the item.

Statuses in use: `open`, `done`, `dropped`, `question`. `question` means the item is waiting on a
person, not on work — it keeps the item out of the loop's reach.

## Guards that are not the build

CI runs three checks over prose, and each exists because something drifted silently:

- `scripts/backlog_index.py --check` — the index against the items (`test -d docs/backlog` first,
  because with no directory the script exits 0 and a lost backlog scores as a pass).
- `scripts/diagnostics_documented.py` — the number of `logger.*` call sites against the number
  `TelekLogger`'s KDoc states. It counts; it cannot tell whether the descriptions still fit, and it
  has already been green over a stale one.
- `scripts/kotlin_version_documented.py` — the README's Kotlin badge against `ci/consumer`'s pin.

## Release

Version head in `gradle.properties`; CI appends its run number, so `0.4.0` is a name and
`0.4.0.72` is the coordinate that resolves. Artifacts are snapshots on
`https://reposilite.kotlin.website/snapshots`, and that is the plan rather than a stopgap —
publishing to Maven Central was considered and **dropped** (`docs/backlog/B-05-maven-central.md`
says why, and what to read first if it is ever reopened). There is no `sborka.central` flag here;
adding it back is one line and a decision.

Releasing is by hand and small: tag the **published** version (`v0.4.0.72`, not `v0.4.0` — a tag
spelling a version nobody can resolve points at nothing) and make its GitHub Release body that
version's section of `RELEASE_NOTES.md`. Check every module is at that version before tagging: a
half-published version looks exactly like a wrong one from outside, and a tag makes it permanent.

Renovate extends sborka's presets; majors get a human. `com.squareup.retrofit2:retrofit` is
switched off for a reason written in `.github/renovate.json5` — it follows kotlin-telegram-bot's own
POM, and no check here would notice it being wrong.

Every release note entry says what breaks and why the break is worth it. Kotlin/Native forbids a
comma inside a backticked test name — legal on the JVM, so a green local run means nothing.
