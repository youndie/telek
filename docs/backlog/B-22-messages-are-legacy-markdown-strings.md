---
id: B-22
title: "Every message goes out as legacy Markdown, and the mode belongs to the handler rather than the message"
status: open
priority: P2
size: L
stage: stage-3-debts
---

# B-22 — Every message goes out as legacy Markdown, and the mode is not the message's to choose

`SendMessageEffect` carries `text: String`, and how Telegram is asked to read that string is decided
by the handler: `parseMode = MarkdownParseMode` in `:ktg`'s send and edit handlers, `ParseMode.MARKDOWN`
in `:telegram`'s two. Legacy Markdown is the Bot API's backward-compatibility mode — bold, italic,
code, pre, links, and nothing else. `underline`, `strikethrough`, `spoiler`, `blockquote` and
`expandable_blockquote` cannot be expressed through telek at all, and not because Telegram lacks
them: because of a constant in four files. `TelegramTextBuilder` sits exactly at that ceiling —
`text`, `bold`, `br`, `br2`, `row`, `list`, returning a `String`, duplicated verbatim across the two
transports.

The ceiling is only half the cost. Because the payload is a string that Telegram parses, any foreign
text inside it — a name a person typed — can open markup that never closes, and Telegram then rejects
the **whole** message; from outside that looks like a button that does nothing. A consumer can answer
that in exactly two ways, escaping by hand at every call site or turning markup off for the whole
message, and neither is a fix: the parse mode is not a parameter, so the choice is not theirs to
make.

- **The decision and its reason.** Send `entities` instead of `parse_mode`, and make the effect carry
  a document rather than a string. Entities are offsets: nothing in the text is parsed, so there is
  nothing to escape, and the defect class disappears by construction rather than by vigilance —
  while every format Telegram has becomes expressible at once. The transport already has the whole
  set in `commonMain`: ktgbotapi's `EntitiesBuilder` (`bold`, `italic`, `underline`, `strikethrough`,
  `spoiler`, `code`, `pre`, `link`, `blockquote`, `expandableBlockquote`, `customEmoji`) plus
  `sendMessage` / `editMessageText` overloads taking `List<TextSource>` addressed by chat and message
  id. Those symbols are present in the `linuxX64` klib, so the native targets keep working, and
  kotlin-telegram-bot accepts `entities: List<MessageEntity>` too — `:telegram` can follow instead of
  being stranded at the old ceiling.
- The rejected alternative is to keep the string and move the constant to `MarkdownV2`. It lifts the
  ceiling and keeps the defect: MarkdownV2 escapes eighteen characters where legacy Markdown escapes
  four, so every escaping a consumer wrote becomes silently wrong on the release that flips it. If
  it is ever done it must be a builder that escapes whatever callers pass and emits markup only from
  its own methods — never a flag a consumer can set on today's `String`.
- Also rejected: a template engine over strings. Markup by offset and substitution into a string are
  incompatible — interpolation moves the offsets — so a template layer would reintroduce a parser and
  a second escaping problem in the same place the first one was removed from. Interpolation that is
  actually needed (a translated phrase with a bold slot) is filling a slot with a *piece*, not with a
  string, and that is a builder feature rather than a language.
- Not covered — and this item has to decide it in the open: where the document model lives. Two
  shapes. A sealed set of pieces in `:core`, mapped per transport: `:core` stays transport-neutral,
  `:telegram` can keep parity, and the cost is a mapping plus a second set of names for things
  ktgbotapi has already named. Or `List<TextSource>` straight in the public signature: no mapping at
  all, and `InlineKeyboardMarkup` already stands in `sendMessage`'s signature so the precedent
  exists — but then `:core`'s API names a client library.
- Not covered: custom emoji. The Bot API gates them behind a username bought on Fragment or the bot
  owner's Premium, so they cannot be part of an API this library offers to everyone.

- AC: a bot on `:ktg` sends a message whose body is an expandable blockquote and whose rows are
  monospace, without naming a `TextSource` or a parse mode itself.
- AC: a message containing a name with `_`, `*`, `[` and a backtick arrives intact with no escaping
  anywhere in the caller's code, and the test that proves it asserts on the pieces rather than on a
  rendered string.
- AC: the document type exposes a plain-text projection, so consumer tests that compare what was said
  against a string keep working across the migration.
- AC: the `String` path stays for one release, deprecated, and the ABI dump carries the new API.
- AC: whichever of the two shapes is chosen, the reason is written here — including what the loser
  would have cost — because the diff will only show the winner.
- Anchors: `ktg/src/commonMain/kotlin/io/github/youndie/telek/ktg/TelegramMessageDsl.kt`,
  `ktg/src/commonMain/kotlin/io/github/youndie/telek/ktg/KtgTransitions.kt`,
  `ktg/src/commonMain/kotlin/io/github/youndie/telek/ktg/effect/Effects.kt`,
  `ktg/src/commonMain/kotlin/io/github/youndie/telek/ktg/effect/handler/SendMessageEffectHandler.kt`,
  `ktg/src/commonMain/kotlin/io/github/youndie/telek/ktg/effect/handler/EditMessageEffectHandler.kt`,
  `telegram/src/main/kotlin/io/github/youndie/telek/telegram/TelegramMessageDsl.kt`,
  `telegram/src/main/kotlin/io/github/youndie/telek/telegram/effect/handler/`.
