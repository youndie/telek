---
id: B-04
title: "/cmd@botname and /cmd with an argument reach no dispatcher"
status: done
priority: P1
size: S
stage: stage-0-input-model
---

# B-04 — /cmd@botname and /cmd with an argument reach no dispatcher

`DefaultFindDispatcherStrategy` strips the leading slash and compares the **whole remaining text** to
`startCommand` (`core/src/commonMain/kotlin/io/github/youndie/telek/Telek.kt`):

```kotlin
val cmd = input.text.removePrefix("/")
dispatchers.firstOrNull { it.startCommand == cmd }
```

So `/start@mybot` looks for a dispatcher whose `startCommand` is `start@mybot`, and `/start ABC-123`
for one called `start ABC-123`. Neither exists, so both fall through to the `"*"` dispatcher or to
routing by state. The first form is not optional in a group: Telegram appends `@botname` whenever
more than one bot can see the message, which is exactly the case [B-01](B-01-conversation-key.md)
makes telek usable for. The second is how a deep link or a shared identifier enters a bot at all.

- **The decision and its reason.** Parse the command properly in one place — strip `@botname`,
  split the argument off, and give the dispatcher the argument. In one place because the alternative
  is every bot re-implementing `substringBefore('@')` in its own `entry`, which is the kind of thing
  a router is supposed to have removed.
- Whether the bot's own username should be *checked* rather than merely stripped is part of the
  item: stripping any `@…` means a bot answers a command addressed to a different bot in the same
  group. Checking it means telek has to know its own username, which it currently does not.
- The rejected alternative is to leave this to `canHandleCallback`-style overriding per dispatcher.
  Rejected because it is the default path that is wrong, and a default that is wrong everywhere is
  not a customisation point.
- Not covered: command arguments typed into anything richer than a `String`.

- AC: `/start@mybot` in a group reaches the same dispatcher as `/start` in a private chat.
- AC: `/start ABC-123` reaches the `start` dispatcher, and the dispatcher can read `ABC-123`.
- AC: a command addressed to another bot's username is either ignored or documented as not ignored —
  with the reason, not by omission.
- Anchors: `core/src/commonMain/kotlin/io/github/youndie/telek/Telek.kt`,
  `core/src/commonMain/kotlin/io/github/youndie/telek/StateDispatcher.kt`,
  `router/src/commonMain/kotlin/io/github/youndie/telek/router/Routes.kt`.

## Iteration 1 — 2026-09-17

Done. `Message.asCommand()` returns a `Command(name, addressedTo, argument)`, and
`DefaultFindDispatcherStrategy` routes on `command.name`. A dispatcher reads its argument from the
same place, so nothing about `entry` or `StateDispatcher` had to change.

- **Split at the first whitespace, not at a space.** A command pasted with a newline after it is
  still that command, and Telegram's own clients produce exactly that.
- **The third criterion was a decision, and it is made where it can be made.** telek cannot know its
  own username, so it cannot decide alone whether `/start@someoneelse` is for it. The strategy takes
  an optional `botUsername`: unset — the default — any addressed command is answered, which is right
  in a private chat and in a group with one bot and wrong in a group with two; set, an
  elsewhere-addressed command stops being a command here and reaches the current state's dispatcher
  as the ordinary message it is, rather than vanishing. Both halves are tested, and the default's
  wrong case is written down rather than left to be discovered.
- **A binary break, named rather than smoothed over.** `DefaultFindDispatcherStrategy`'s
  single-argument constructor is gone from the ABI, replaced by one with a defaulted second
  parameter; Kotlin source is unaffected. A secondary constructor would have preserved it, and was
  not added: nothing is on Maven Central yet ([B-05](B-05-maven-central.md) is a question), so there
  is nothing linked against the old binary to protect, and the constructor would outlive the reason
  for its existence. `RELEASE_NOTES.md` says so.
- **The first mutation was too coarse and was replaced.** Making the strategy never parse a command
  failed eleven tests including every wizard entry — proof the mechanism is exercised, but no
  isolation of what this item added. Removing only the `@`-suffix split fails exactly six, all six
  about the suffix, while bare commands, the argument and every wizard stay green.
