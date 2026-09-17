# telek-consumer

A bot-shaped project that depends on telek the way a stranger does: by coordinate and repository
URL. It shares nothing with the build above it — not the version catalogue, not a source set, not a
project dependency — because those are the things publication breaks, and a module inside the
repository has all three and can notice none of them.

Run it against a published version, with a cache created for the run:

```bash
COLD=$(mktemp -d /tmp/telek-cold-XXXX)
./gradlew --no-daemon -g "$COLD" -Ptelek.version=<version> build
./build/bin/linuxX64/releaseExecutable/telek-consumer.kexe
```

`-g "$COLD"` is the point of the exercise. A warm `~/.gradle` can serve an artefact that was never
published, or a stale one under a version that has since moved; a cache made for the run cannot.

Aim it elsewhere with `-Ptelek.repo=<url>` — a directory on disk holds the same bytes a server
will, so a candidate publication can be checked before it goes out, and Maven Central needs no
change here beyond that property.

**It is meant to fail.** Two of the three things it found on its first runs are filed as
[B-18](../../docs/backlog/B-18-published-api-is-hidden-behind-implementation.md) and
[B-19](../../docs/backlog/B-19-telegram-needs-a-repository-nobody-mentions.md), and worked around
here with a comment naming the item — so the harness keeps running and the defect stays visible.
Deleting those workarounds is the acceptance for those items.
