#!/usr/bin/env python3
"""
Builds the backlog index in backlog.md out of the frontmatter of the item files.

    python3 scripts/backlog_index.py                        # rewrite the index
    python3 scripts/backlog_index.py --check                # verify only (CI)
    python3 scripts/backlog_index.py --against origin/main  # the number is free on that ref (CI on a PR)

LAYOUT. Items are one file each in `<docs>/backlog/`; the index they are collected into is
`backlog.md` next to `docs/`, at the root of the project. `--docs PATH` moves the tree (default:
`docs` relative to the working directory), `--backlog PATH` moves the index file. The two are
separate on purpose: the index is not only an index — the goal, the reality check and the product
decisions live in the same file, above the generated block, and that text belongs to the project,
not to the documentation tree.

WHY THE INDEX IS GENERATED and the items are not. The status of a task changes in the task's own
file, and the only way to keep an index from drifting away from the tasks it indexes is to refuse
to store the status twice. Everything between the BEGIN INDEX / END INDEX markers is rewritten;
the rest of backlog.md is hand-written and never touched.

Besides the index the script guards four things:

  * `id` equals the file name;
  * a number is not used twice;
  * `blocked_by` points at a task that exists;
  * **a slug is not used twice** — one task must not sit under two numbers.

The last one exists because the duplicate-`id` check cannot see it: two files carrying the same
text under different numbers have different ids, and each number is legal on its own. It happens
when a task is renumbered on one branch while a second branch merges the same task under the old
number.
"""
import argparse
import os
import re
import subprocess
import sys

try:
    import yaml
except ImportError:
    sys.exit("PyYAML is required:  pip install pyyaml")

# The markers are matched, not hard-coded, so that a project can word the "do not edit by hand"
# note however it likes; whatever is in the file is written back unchanged.
BEGIN_RE = re.compile(r"<!--\s*BEGIN INDEX.*?-->", re.S)
END_RE = re.compile(r"<!--\s*END INDEX\s*-->")

MARK = {"open": "`[ ]`", "wip": "`[~]`", "done": "`[x]`",
        "question": "`[?]`", "dropped": "`[-]`"}

PRIO_ORDER = {"P0": 0, "P1": 1, "P2": 2, "P3": 3, "infra": 4, None: 5}

# What counts as still on the table. `dropped` is not: a refused item is kept as a file so that the
# same proposal is refused in ten seconds the next time it comes up, but listing it among the open
# work would make the backlog look longer than it is. It is closed, and the mark says how.
OPEN_STATUSES = ("open", "wip", "question")

ITEM_FILE = re.compile(r"^B-\d+-.*\.md$")

# A row of a hand-written table whose first cell is a stage id: `| stage-id | Human name | ... |`.
STAGE_ROW = re.compile(r"^\|\s*`?([A-Za-z0-9][A-Za-z0-9._-]*)`?\s*\|\s*([^|]*?)\s*\|", re.M)

# Links from an item to a document. Same shape as in the documents themselves: relative, .md,
# possibly with an anchor.
LINK = re.compile(r"\]\((?!https?:|#)([^)#]+\.md)(?:#[^)]*)?\)")


def _utf8_stdout():
    """The output of this script is ASCII, but ids and titles come out of the documents, and a
    project's documentation may be written in any language. A Windows console defaults to a legacy
    code page and dies on the first character outside it; CI on Linux never sees this, so the
    failure looks like a script that is broken on exactly one developer's machine — `--check` blows
    up with a traceback before it manages to report anything."""
    for stream in (sys.stdout, sys.stderr):
        if hasattr(stream, "reconfigure"):
            try:
                stream.reconfigure(encoding="utf-8", errors="replace")
            except (ValueError, OSError):
                pass


def parse(path):
    """One item file → its frontmatter, with the three fields the index needs verified."""
    name = os.path.basename(path)
    with open(path, encoding="utf-8") as fh:
        text = fh.read()

    m = re.match(r"^---\n(.*?)\n---\n", text, re.S)
    if not m:
        sys.exit("{0}: no frontmatter".format(name))
    try:
        fm = yaml.safe_load(m.group(1)) or {}
    except yaml.YAMLError as e:
        sys.exit("{0}: frontmatter does not parse: {1}".format(name, e))
    if not isinstance(fm, dict):
        sys.exit("{0}: frontmatter is not a mapping".format(name))

    for key in ("id", "title", "status"):
        if key not in fm or fm[key] in (None, ""):
            sys.exit("{0}: required field {1} is missing".format(name, key))

    fm["id"] = str(fm["id"]).strip()
    fm["title"] = str(fm["title"]).strip()
    if not name.startswith(fm["id"] + "-"):
        sys.exit("{0}: the file name does not match id {1}".format(name, fm["id"]))
    if fm["status"] not in MARK:
        sys.exit("{0}: unknown status {1} (expected one of {2})"
                 .format(name, fm["status"], ", ".join(sorted(MARK))))

    dep = fm.get("blocked_by")
    if dep is None:
        fm["blocked_by"] = []
    elif not isinstance(dep, list):
        fm["blocked_by"] = [str(dep)]
    else:
        fm["blocked_by"] = [str(d).strip() for d in dep if str(d).strip()]

    fm["path"] = path
    return fm


def link(item):
    return "[{0}]({1})".format(item["id"], item["file"])


def stage_order(hand_written, items):
    """Stage ids in the order a person put them into backlog.md, with their headings.

    A stage is a field on the item, not a directory, so nothing on disk says that "packaging"
    comes after "blockers", nor what either is called in prose. That order and those names are
    editorial, so they are read out of the hand-written part of backlog.md: the first cell of a
    table row that holds a stage id gives the stage its heading. A stage the file does not mention
    is appended in alphabetical order rather than dropped — an item must never disappear from the
    index because someone forgot to describe its stage.
    """
    used = {i.get("stage") for i in items if i.get("stage")}
    ordered, seen = [], set()
    for slug, title in STAGE_ROW.findall(hand_written):
        if slug in used and slug not in seen:
            seen.add(slug)
            ordered.append((slug, title.strip() or slug))
    ordered += [(slug, slug) for slug in sorted(used - seen)]
    ordered.append((None, "No stage"))
    return ordered


def render(items, stages, begin, end):
    out = [begin, ""]

    live = [i for i in items if i["status"] in OPEN_STATUSES]
    live.sort(key=lambda i: (PRIO_ORDER.get(i.get("priority"), 5), i["id"]))

    out += ["## Open ({0})".format(len(live)), ""]
    if live:
        out += ["| Task | | Priority | Size | Blocked by |", "|---|---|---|---|---|"]
        for i in live:
            blockers = ", ".join(i["blocked_by"]) or "-"
            out.append("| {0} {1} | {2} | {3} | {4} | {5} |".format(
                link(i), MARK[i["status"]], i["title"],
                i.get("priority", "-"), i.get("size", "-"), blockers))
    else:
        out.append("No open tasks.")

    closed = [i for i in items if i["status"] not in OPEN_STATUSES]
    out += ["", "## Closed ({0})".format(len(closed)), ""]
    for slug, title in stages:
        chunk = sorted([i for i in closed if i.get("stage") == slug], key=lambda i: i["id"])
        if not chunk:
            continue
        out += ["**{0}**".format(title), ""]
        out += ["- {0} {1} - {2}".format(link(i), MARK[i["status"]], i["title"]) for i in chunk]
        out.append("")

    out += [end]
    return "\n".join(out).rstrip("\n") + "\n"


def check_links(items_dir, files):
    """
    Links from an item to a document are relative, and item files sit one level below the tree
    they link into. A missing `../` breaks nothing locally and is caught by no test: the link
    simply 404s when someone opens it in a web UI, and the person who notices is the reader, not
    the author. Broken links pile up in a backlog faster than anywhere else, because nobody reads
    an item twice.
    """
    broken = []
    for name in files:
        path = os.path.join(items_dir, name)
        with open(path, encoding="utf-8") as fh:
            for target in LINK.findall(fh.read()):
                if not os.path.exists(os.path.normpath(os.path.join(items_dir, target))):
                    broken.append("  {0} -> {1}".format(name, target))
    if broken:
        sys.exit("broken links in backlog items:\n" + "\n".join(sorted(set(broken))))


def check_against(ref, items_dir):
    """
    Catches a number that is already taken: an id that was free when the branch was cut but has
    since been used by another task that merged first.

    An ordinary duplicate check cannot see this — inside each branch on its own there is no
    duplicate, it appears only after the **second** merge. Two pull requests carrying the same
    number are both green and the default branch is what turns red, after the fact, with the wrong
    number already quoted in whatever code and documents referenced the task.

    The comparison is by file name rather than by frontmatter: the same number under a different
    slug is exactly what "the number is taken" means, and `id` matching the file name is verified
    separately in parse().
    """
    try:
        out = subprocess.run(
            ["git", "ls-tree", "-r", "--name-only", ref, "--", "."],
            cwd=items_dir, capture_output=True, text=True, check=True,
        ).stdout
    except (subprocess.CalledProcessError, FileNotFoundError, OSError) as e:
        sys.exit("could not read {0}: {1}".format(ref, e))

    theirs, their_slugs = {}, {}
    for path in out.splitlines():
        name = os.path.basename(path)
        m = re.match(r"^(B-\d+)-(.*\.md)$", name)
        if m:
            theirs[m.group(1)] = name
            their_slugs[m.group(2)] = name

    ours = sorted(f for f in os.listdir(items_dir) if ITEM_FILE.match(f))

    taken = []
    for name in ours:
        num = re.match(r"^(B-\d+)-", name).group(1)
        other = theirs.get(num)
        if other and other != name:
            taken.append("  {0}: here {1}, on {2} already {3}".format(num, name, ref, other))
    if taken:
        sys.exit("this number is already taken on {0} - take a free one and rename the file:\n"
                 .format(ref) + "\n".join(taken))

    # The mirror case: the same slug under a different number. This is how one task ends up on the
    # default branch twice - it was renumbered on one branch while another brought it in under the
    # previous number, and every number-based check stayed silent, because the numbers do differ.
    renamed = []
    for name in ours:
        slug = name.split("-", 2)[2]
        other = their_slugs.get(slug)
        if other and other != name:
            renamed.append("  {0}: here {1}, on {2} already {3}".format(slug, name, ref, other))
    if renamed:
        sys.exit("this task already exists on {0} under a different number - take the number from "
                 "there, otherwise after the merge it will sit in the backlog twice:\n".format(ref)
                 + "\n".join(renamed))
    return 0


def main():
    ap = argparse.ArgumentParser(description="Generate the backlog index inside backlog.md")
    ap.add_argument("--docs", metavar="PATH", default="docs",
                    help="the docs/ tree (default: docs, relative to the working directory)")
    ap.add_argument("--backlog", metavar="PATH",
                    help="the index file (default: <docs>/../backlog.md)")
    ap.add_argument("--check", action="store_true",
                    help="verify only, write nothing; exit 1 if the index is stale")
    ap.add_argument("--against", metavar="REF",
                    help="check that no item number is already taken on REF (a git ref)")
    args = ap.parse_args()

    _utf8_stdout()

    docs = os.path.abspath(args.docs)
    items_dir = os.path.join(docs, "backlog")
    index_path = (os.path.abspath(args.backlog) if args.backlog
                  else os.path.join(os.path.dirname(docs), "backlog.md"))

    # A missing tree is a mode, not a failure: a project may have no backlog yet. Saying so is not
    # the same as saying that everything is in order, so it is said out loud.
    if not os.path.isdir(items_dir):
        print("no backlog items at {0} - nothing checked".format(items_dir))
        return 0

    if args.against:
        return check_against(args.against, items_dir)

    files = sorted(f for f in os.listdir(items_dir) if ITEM_FILE.match(f))
    items = [parse(os.path.join(items_dir, f)) for f in files]

    index_dir = os.path.dirname(index_path) or "."
    for it in items:
        it["file"] = os.path.relpath(it["path"], index_dir).replace(os.sep, "/")

    ids = [i["id"] for i in items]
    dupes = {i for i in ids if ids.count(i) > 1}
    if dupes:
        sys.exit("duplicate ids: " + ", ".join(sorted(dupes)))

    # The same text under two numbers. The check above cannot see it: the ids differ and each one
    # is legal on its own. It appears when a task is renumbered on one branch and merged in
    # parallel from another.
    by_slug = {}
    for f in files:
        by_slug.setdefault(f.split("-", 2)[2], []).append(f)
    slug_dupes = {slug: fs for slug, fs in by_slug.items() if len(fs) > 1}
    if slug_dupes:
        lines = ["  {0}: {1}".format(slug, ", ".join(sorted(fs)))
                 for slug, fs in sorted(slug_dupes.items())]
        sys.exit("one task under several numbers:\n" + "\n".join(lines))

    known = set(ids)
    for i in items:
        for dep in i["blocked_by"]:
            if dep not in known:
                sys.exit("{0}: blocked_by points at {1}, which does not exist".format(i["id"], dep))

    check_links(items_dir, files)

    if not os.path.isfile(index_path):
        sys.exit("no index file at {0} - create it with the BEGIN INDEX / END INDEX markers, "
                 "or point --backlog at it".format(index_path))
    with open(index_path, encoding="utf-8") as fh:
        text = fh.read()

    begin, end = BEGIN_RE.search(text), END_RE.search(text)
    if not begin or not end or end.start() < begin.end():
        sys.exit("{0} has no BEGIN INDEX / END INDEX markers"
                 .format(os.path.basename(index_path)))

    head = text[:begin.start()]
    tail = text[end.end():]
    stages = stage_order(head + tail, items)
    updated = head + render(items, stages, begin.group(0), end.group(0)).rstrip("\n") + tail

    if args.check:
        if updated != text:
            sys.exit("the index in {0} is stale - run scripts/backlog_index.py"
                     .format(os.path.basename(index_path)))
        print("index is up to date: {0} items".format(len(items)))
        return 0

    if updated != text:
        with open(index_path, "w", encoding="utf-8") as fh:
            fh.write(updated)
    print("index written: {0} items".format(len(items)))
    return 0


# Guarded, because the tables above (the status marks, the priority order) and parse() are worth
# importing: a site builder or a report can reuse them instead of re-deriving them. Without the
# guard, importing this module would silently rewrite backlog.md as a side effect.
if __name__ == "__main__":
    sys.exit(main())
