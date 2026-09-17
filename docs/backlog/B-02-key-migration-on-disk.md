---
id: B-02
title: "FileStateStorage names each file after the key, so changing the key rewrites the disk"
status: wip
priority: P1
size: S/M
stage: stage-0-input-model
blocked_by: [B-01]
---

# B-02 — FileStateStorage names each file after the key, so changing the key rewrites the disk

`FileStateStorage` writes `$chatId.json` and renames `$chatId.json.tmp` over it
(`persistence/src/commonMain/kotlin/io/github/youndie/telek/persistence/FileStateStorage.kt`). The
key is literally the file name, so [B-01](B-01-conversation-key.md) is not only an API change: every
bot that has run with `:persistence` has a directory whose layout stops matching the key that looks
for it. A file that is no longer found reads as a user who lost their place mid-wizard, silently —
`load` returning `null` is indistinguishable from a first-time user.

- **The decision and its reason.** Decide deliberately between a migration on read (an old-shaped
  file is found under its old name, loaded and rewritten under the new one) and a documented break
  in the release notes, the way 0.3.0 already broke stored state and said so. The reason to decide
  it here rather than inside B-01 is that the answer depends on how many bots exist — today the
  honest answer is likely "none outside this repository", and that makes the break correct and free.
  It stops being free the moment [B-07](B-07-real-bot-in-production.md) runs somewhere.
- The rejected alternative is to say nothing and let it break. Rejected on precedent: 0.3.0 already
  broke stored state and spent four paragraphs of `RELEASE_NOTES.md` on it, which is the standard
  this repository has set for itself.
- Not covered: `UserStateStore` implementations a consumer wrote themselves. They break, and the
  release notes say so.

- AC: a directory written by the previous version is either migrated on first read, or the release
  notes state plainly that it is not and what to do instead — and a test covers whichever was chosen.
- AC: whichever is chosen, a stale file is never silently treated as "no state".
- Anchors: `persistence/src/commonMain/kotlin/io/github/youndie/telek/persistence/FileStateStorage.kt`,
  `persistence/src/commonMain/kotlin/io/github/youndie/telek/persistence/PersistableUserStateStoreImpl.kt`,
  `RELEASE_NOTES.md`.
