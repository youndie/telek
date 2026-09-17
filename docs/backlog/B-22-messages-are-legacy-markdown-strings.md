---
id: B-22
title: "Every message goes out as legacy Markdown, and the mode belongs to the handler rather than the message"
status: done
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

## Iteration 1 — 2026-09-18

Done. Entities instead of `parse_mode`, a document instead of a string, and the shape question
answered against evidence rather than by preference.

### The shape: a sealed set in `:core`, and the question above was framed wrong

This item offered two shapes and priced the second as "`:core`'s API names a client library". That
premise does not hold: **`:core` has no message effect at all.** `SendMessageEffect` is declared in
`:ktg` and again in `:telegram`, and each already names its own client's `InlineKeyboardMarkup`. So
the choice was never "does `:core` name ktgbotapi"; it was whether the two transports share a
document type or each uses its client's own.

Sharing, for three reasons that survived checking:

- **`:core` already models Telegram's domain.** `Input` carries `Message`, `Photo`, `Document`,
  `Contact`, `Location`, `FileRef`. A message body is the outgoing mirror of that, not a new kind of
  dependency, and `MessageText` names Telegram rather than a library exactly as `Input` does.
- **The alternative does not avoid the mapping, it duplicates it.** kotlin-telegram-bot takes
  `(text, entities)` with UTF-16 offsets, so `:telegram` needs a flattener whichever shape wins.
  Per-transport types would have bought one saved mapping in `:ktg` and cost a second builder, a
  second flattener, and a second set of names.
- **It removes a duplication that was already a known wart.** `TelegramTextBuilder` existed twice,
  verbatim, and its own comment asked the next person to keep the copies in sync — a job nothing
  checked. There is now one `MessageTextBuilder` in `:core`; both copies are deleted, and the tests
  that guarded their layout behaviour were ported into `:core` rather than dropped.

The loser's cost, since the diff only shows the winner: `List<TextSource>` in `:ktg`'s signatures
would have made `:ktg` slightly thinner — thirty lines of mapping gone — and stranded `:telegram` at
the old ceiling or given it a parallel, differently-named API. The ceiling was the whole complaint.

### What shipped

- `:core` — `MessageText`, `TextPiece` (`Plain`, `Styled`, `Link`, `Code`, `CodeBlock`), `TextStyle`,
  `MessageTextBuilder` with `message { }`, `plain`, and `entities()`.
- `:ktg` — `MessageText.asTextSources()`; both handlers send `entities` and name no parse mode.
- `:telegram` — `MessageText.asMessageEntities()`; both handlers send `(text, entities)`.
- The deprecated path is a **type**, not a flag: `SendMarkdownMessageEffect` /
  `EditMarkdownMessageEffect` in each transport, registered by default, with the string overloads of
  `sendMessage` / `editMessage` deprecated and pointing at the replacement. It goes by deleting two
  effects, two handlers and two registry lines — not by finding a branch.
- Kept as legacy Markdown rather than reinterpreted as literal text, deliberately: had the string
  overload started sending its argument literally, every asterisk in an upgrading bot's messages
  would have become a character on upgrade and nothing would have failed.

### The criteria

- **Expandable blockquote and monospace without naming a `TextSource` or a parse mode** — the README
  sample does exactly that, compiled in `:docs-samples`.
- **A name with `_`, `*`, `[` and a backtick, no escaping, asserted on pieces** — three times, at
  three levels: `:core`'s `MessageTextTest`, each transport's mapping test, and `ci/consumer`, which
  resolves telek from a published coordinate and knows nothing else about it.
- **A plain-text projection** — `MessageText.plain`. The consumer tests that compared rendered
  strings were migrated to it in one substitution, which is the point of having it.
- **The `String` path stays one release, deprecated, and the ABI dump carries the new API** — both
  done; `-Werror` in `:example` and `:docs-samples` turned the deprecation into a build failure, so
  telek's own samples migrated in this change rather than later.
- **The decision written here** — above.

### Two things worth carrying

- **UTF-16, not characters.** Telegram counts entity offsets in UTF-16 code units. Kotlin's `String`
  already indexes that way, so the arithmetic is right by default — and wrong the moment somebody
  "fixes" it to count characters. There is a test with an astral emoji whose only job is to fail
  then, because every message without one passes either way.
- **The example was already carrying the defect.** It edited a message containing text the user had
  typed and sent a cat fact from somebody else's API, both as Markdown strings. Neither was a
  hypothetical; both are now documents, with the reason written beside them.
