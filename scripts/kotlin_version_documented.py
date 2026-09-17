#!/usr/bin/env python3
"""
Checks that the Kotlin version README's badge names is the one telek is actually compiled with.

    python3 scripts/kotlin_version_documented.py

WHY THIS EXISTS. telek does not hold its own Kotlin version: it comes from the shared `wip`
catalogue, so a bump lands here without a line of this repository changing. The README badge is a
number written by hand beside one that moves somewhere else, and the two had already drifted —
the badge said 2.4.10 while the build ran 2.4.20 — with nothing red anywhere.

WHERE THE TRUTH COMES FROM. `ci/consumer` pins the Kotlin plugin to what telek was compiled with,
and that pin is not prose: Kotlin metadata is one-directional, so a consumer on an older compiler
cannot read telek's classes at all. If the pin is wrong, the consumer build fails. So the pin is
enforced and the badge is not, and this compares the unenforced one against the enforced one.

WHAT IT CANNOT DO. It cannot tell either of them is *right*. If the catalogue moves to 2.5.0 and
nobody touches the consumer, the consumer build is what fails — this check stays green, because
both numbers still agree with each other. It guards drift between two places, not truth.
"""
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
README = ROOT / "README.md"
CONSUMER = ROOT / "ci/consumer/build.gradle.kts"

BADGE = re.compile(r"img\.shields\.io/badge/Kotlin-(\d+\.\d+\.\d+)-")
PIN = re.compile(r'kotlin\("multiplatform"\)\s+version\s+"(\d+\.\d+\.\d+)"')


def one(pattern, path, what):
    found = pattern.findall(path.read_text(encoding="utf-8"))
    if not found:
        sys.exit(f"{path.relative_to(ROOT)}: no {what} found — this check lost its subject")
    if len(set(found)) > 1:
        sys.exit(f"{path.relative_to(ROOT)}: {what} disagrees with itself: {sorted(set(found))}")
    return found[0]


badge = one(BADGE, README, "Kotlin badge version")
pin = one(PIN, CONSUMER, "Kotlin plugin pin")

if badge != pin:
    sys.exit(
        f"README's Kotlin badge says {badge}; ci/consumer pins {pin}.\n"
        f"The pin is the enforced one — a consumer on the wrong compiler cannot read telek's "
        f"metadata at all. Fix the badge, or fix the pin if the toolchain moved."
    )

print(f"Kotlin {badge}: README badge and ci/consumer pin agree")
