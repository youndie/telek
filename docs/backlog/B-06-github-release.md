---
id: B-06
title: "Tag the release and put the notes where a stranger looks for them"
status: done
priority: P1
size: XS
stage: stage-1-release
---

# B-06 — Tag the release and put the notes where a stranger looks for them

`RELEASE_NOTES.md` is good and it is thorough — 0.3.0 spends four paragraphs on exactly how stored
state breaks and what to do about it. It is also invisible: the repository has no tags and no
GitHub Releases, so the place every consumer of every other library looks is empty, and the notes
are found only by someone already reading the source tree.

- **The decision and its reason.** A tag per released version and a GitHub Release whose body is
  that version's section of `RELEASE_NOTES.md`. The reason is not ceremony: a release is the only
  artefact that dates a version, and a consumer deciding whether to upgrade reads the breaking
  paragraph *before* they resolve the dependency, not after their state store stops loading.
- The rejected alternative is generated release notes from commit subjects. The conventional-commit
  subjects are good enough to generate something readable, and that something would still not
  contain the four paragraphs about `classDiscriminator` — which is the entire value of the file.
- Not covered: automating it. One dispatch and one paste per release is not a problem worth a
  workflow until there are more releases than there are today.

- AC: a tag exists for a version that is actually published, and its GitHub Release body is that
  version's section of `RELEASE_NOTES.md`.
- Anchors: `RELEASE_NOTES.md`.

## Iteration 1 — 2026-09-17

Done. [`v0.4.0.72`](https://github.com/youndie/telek/releases/tag/v0.4.0.72) tags `a966cff`, and
the Release body is that version's section of `RELEASE_NOTES.md`.

- **The blocker was never real, and it came from the AC rather than from the work.** This item
  waited on [B-05](B-05-maven-central.md) because its own acceptance criterion said "the version
  published in B-05". A tag and a Release need a published version, not a particular registry, and
  there had been one all along. B-05 is dropped; this took twenty minutes. Rewritten to say what it
  actually needs.
- **The tag names `0.4.0.72`, not `0.4.0`.** The head of the version lives in `gradle.properties`
  and CI appends its run number, so `0.4.0` is a name no consumer can resolve — a tag spelling it
  would point at nothing, which is the failure this item exists to prevent. The Release is *titled*
  `0.4.0`, because that is what the notes section is called, and its first line gives the coordinate
  that resolves.
- **Checked before tagging, not after:** all eight published modules are at `0.4.0.72`. A half
  published version looks exactly like a wrong one from the outside, and a tag would have made that
  permanent.
- Not automated, and still deliberately: one tag and one paste per release is not worth a workflow
  at this rate. If that changes, the body is a section of a file with a stable heading — the
  cheapest possible thing to generate from.
