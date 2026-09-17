---
id: B-06
title: "Tag the release and put the notes where a stranger looks for them"
status: open
priority: P1
size: XS
stage: stage-1-release
blocked_by: [B-05]
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

- AC: a tag exists for the version published in [B-05](B-05-maven-central.md), and its GitHub
  Release body is that version's section of `RELEASE_NOTES.md`.
- Anchors: `RELEASE_NOTES.md`.
