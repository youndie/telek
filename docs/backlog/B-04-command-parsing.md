---
id: B-04
title: "/cmd@botname and /cmd with an argument reach no dispatcher"
status: wip
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
