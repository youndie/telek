#!/usr/bin/env python3
"""
Checks that TelekLogger's KDoc still lists every diagnostic telek emits.

    python3 scripts/diagnostics_documented.py

WHY THIS EXISTS. `TelekLogger`'s documentation names the messages a bot loses by leaving the
default `NoOp` in place, and says how many there are. That is a hand-written list beside a growing
set: the seventh `logger.warn(...)` anybody adds makes the list wrong, and nothing about adding it
would say so. Prose next to code is checked by nobody.

WHAT IT CANNOT DO. It counts call sites; it cannot tell whether the list *describes* them. A
diagnostic that is added and listed with the wrong description passes here. The count is the cheap
half of the problem and the half that fails silently.
"""
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
DOC = ROOT / "core/src/commonMain/kotlin/io/github/youndie/telek/TelekLogger.kt"

# Only main source sets: a diagnostic emitted from a test is a test's business.
MAIN_SOURCES = ("src/commonMain", "src/main", "src/jvmMain", "src/nativeMain")
CALL = re.compile(r"\blogger\.(warn|error|debug|log)\s*\(")
# "none of the six things telek has decided are worth saying"
STATED = re.compile(r"none of the (\w+) things telek has decided")

WORDS = {
    "two": 2, "three": 3, "four": 4, "five": 5, "six": 6, "seven": 7,
    "eight": 8, "nine": 9, "ten": 10, "eleven": 11, "twelve": 12,
}


def main() -> int:
    sites = []
    for path in sorted(ROOT.glob("*/src/**/*.kt")):
        rel = path.relative_to(ROOT).as_posix()
        if not any(part in rel for part in MAIN_SOURCES):
            continue
        for number, line in enumerate(path.read_text(encoding="utf-8").splitlines(), 1):
            if CALL.search(line):
                sites.append("{0}:{1}".format(rel, number))

    # The sentence wraps, and KDoc continuation lines start with " * " -- so the count is matched
    # against the doc with those markers and every run of whitespace collapsed, not against the raw
    # file. Matching raw text made this check pass or fail on where ktlint chose to break a line.
    doc = re.sub(r"\s*\n\s*\*\s*", " ", DOC.read_text(encoding="utf-8"))
    match = STATED.search(doc)
    if not match:
        print("cannot find the count in {0} - did the sentence change?".format(DOC.name), file=sys.stderr)
        return 1

    stated = WORDS.get(match.group(1))
    if stated is None:
        print("unrecognised number word {0!r} in {1}".format(match.group(1), DOC.name), file=sys.stderr)
        return 1

    if stated != len(sites):
        print(
            "TelekLogger says {0} diagnostics; {1} call sites exist:\n  {2}\n"
            "Add the new one to the table in {3} (and say which of the two tables it belongs in -\n"
            "reported nowhere else, or also reachable another way), then update the count."
            .format(stated, len(sites), "\n  ".join(sites), DOC.name),
            file=sys.stderr,
        )
        return 1

    print("TelekLogger documents all {0} diagnostics".format(len(sites)))
    return 0


if __name__ == "__main__":
    sys.exit(main())
