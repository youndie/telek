---
id: B-02
title: "FileStateStorage names each file after the key, so changing the key rewrites the disk"
status: done
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

## Iteration 1 — 2026-09-17

Done, and the choice was the documented break rather than a migration on read — for the reason the
item gives, sharpened by what [B-01](B-01-conversation-key.md) turned up: a chat key's file name is
byte-identical to what every earlier version wrote, so `Keying.PerChat` is not a consolation prize
but a working fallback that reads the old directory exactly as before. Migrating on read would
rewrite files under new names to buy something a one-line configuration change already buys.

The second acceptance criterion carried the actual work. `load` returning `null` for "superseded
file, not found under this key" and for "nobody has ever written here" is the same `null`, and the
upgrade is invisible at every other moment. So `FileStateStorage` now checks, on a miss for a key
that carries a user, whether the chat-keyed file it supersedes exists, and names that path in a
warning. Nothing is migrated, nothing is deleted.

- **Verified against a mutation:** removing the call makes exactly one test fail — the one that
  asserts the warning. The two silence controls (a genuinely new conversation, and a miss on a
  chat key) pass with or without it, which is the point of having them: they show the warning is
  conditional rather than emitted on every miss.
- **The residual gap, and it is not this item's to close:** the warning goes to `TelekLogger`,
  whose default is `NoOp`. A bot that configures no logger is still silent. That is a property of
  the whole library rather than of this storage — the dropped-input warning and failed-effect
  reporting have it too — so it is filed as [B-14](B-14-diagnostics-are-off-by-default.md) and
  said plainly in `RELEASE_NOTES.md` instead of being quietly fixed here.
- **Where it ran:** the WSL box was unreachable for this iteration (mutagen's beta connection
  timed out during banner exchange), so the full multi-target build ran on CI rather than there;
  `:persistence:jvmTest` ran locally, 25 tests, for the mutation check.
