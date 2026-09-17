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

Version head in `gradle.properties`; CI appends the run number. `sborka.central=true` is the whole
build-side contract for Maven Central, but the upload is a manual `workflow_dispatch` in
`youndie/sborka` and it leaves the bundle **staged** — releasing it is a person's click, because a
version on Central can never be rewritten or withdrawn. Publishing to Central is currently deferred;
`docs/backlog/B-05-maven-central.md` holds the decision and what is still open.

Every release note entry says what breaks and why the break is worth it. Kotlin/Native forbids a
comma inside a backticked test name — legal on the JVM, so a green local run means nothing.
