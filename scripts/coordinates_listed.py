#!/usr/bin/env python3
"""
Checks that the snapshot workflow hands proba every coordinate the build publishes, and no other.

    ./gradlew -q --no-configuration-cache -I scripts/publications.init.gradle.kts help \\
      | python3 scripts/coordinates_listed.py

WHY THIS EXISTS. After each snapshot publish, sborka's `publish-wip.yaml` runs proba over the
`coordinates:` it is given, reading each one back from the repository the way a stranger's build
does. That list is written by hand beside a set that grows on its own: a new module, or a new target
on an old one, is a new coordinate, and nothing about adding it would touch the workflow. proba would
then stay green while never looking at the new artifact, which is a check that has quietly stopped
covering what it names.

WHERE THE TRUTH COMES FROM. The init script prints every Maven publication the build registers —
the set `publishAllPublicationsToWipRepository` uploads — one `PUB` line each, on standard input
here. The other side is `jobs.publish.with.coordinates` in the workflow.

WHAT IT CANNOT DO. It compares the list with what the build *intends* to publish, not with what
reached the server; that half is proba's.
"""
import sys
from pathlib import Path

import yaml

ROOT = Path(__file__).resolve().parent.parent
WORKFLOW = ROOT / ".github/workflows/publish-telek-snapshot.yaml"

built = set()
for line in sys.stdin:
    parts = line.rstrip("\n").split("\t")
    if len(parts) == 4 and parts[0] == "PUB":
        built.add(parts[3])

if not built:
    sys.exit(
        "no PUB lines on standard input — the init script did not run, so this would compare "
        "against nothing. Was --no-configuration-cache passed?"
    )

workflow = yaml.safe_load(WORKFLOW.read_text(encoding="utf-8"))
try:
    text = workflow["jobs"]["publish"]["with"]["coordinates"]
except KeyError:
    sys.exit(f"{WORKFLOW.relative_to(ROOT)}: no jobs.publish.with.coordinates — this check lost its subject")

listed = [line.strip() for line in text.splitlines() if line.strip()]
duplicates = sorted({c for c in listed if listed.count(c) > 1})
listed = set(listed)

missing = sorted(built - listed)
extra = sorted(listed - built)
if missing or extra or duplicates:
    message = [f"{WORKFLOW.relative_to(ROOT)} and the build disagree about what is published:"]
    message += [f"  published but not checked by proba: {c}" for c in missing]
    message += [f"  listed but not published by the build: {c}" for c in extra]
    message += [f"  listed twice: {c}" for c in duplicates]
    sys.exit("\n".join(message))

print(f"{len(built)} coordinates: the build and the proba list agree")
