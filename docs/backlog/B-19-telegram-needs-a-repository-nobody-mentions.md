---
id: B-19
title: ":telegram cannot be resolved from the repositories the README names"
status: open
priority: P1
size: S
stage: stage-1-release
---

# B-19 — `:telegram` cannot be resolved from the repositories the README names

`:telegram` and `:router-telegram` depend on
`io.github.kotlin-telegram-bot.kotlin-telegram-bot:telegram:10.0.0`, which is **not on Maven
Central** — checked: 404 there, 200 on JitPack. telek's own `settings.gradle.kts` knows this and
adds JitPack with a content filter, including a comment about the group prefix being
`io.github.kotlin-telegram-bot.…` rather than `com.github.…`.

A consumer is told none of it. `README.md`'s installation section names `mavenCentral()` and the
snapshot repository, and following it exactly produces:

```
Could not find io.github.kotlin-telegram-bot.kotlin-telegram-bot:telegram:10.0.0.
Required by: io.github.youndie.telek:router-telegram, io.github.youndie.telek:telegram
```

So the module that is in maintenance is not merely un-extended — it is **unusable by anyone who
follows the instructions**, and the error names kotlin-telegram-bot rather than telek, so the
person reading it has no reason to suspect the README.

Found by [B-17](B-17-consumer-acceptance-from-outside.md) on its second run, after
[B-18](B-18-published-api-is-hidden-behind-implementation.md) had been worked around. Nothing inside
this repository can see it: telek's own build has the JitPack line.

- **The decision and its reason.** The README's installation section states the repository
  `:telegram` needs, next to the dependency line that needs it. The reason is that this is the only
  document a consumer reads before typing a coordinate, and the failure it prevents does not look
  like telek's fault.
- **This is also a fact [B-05](B-05-maven-central.md) has to answer**, and it is filed here rather
  than there because it is true today, on Reposilite, for anyone using `:telegram`: publishing
  `:telegram` to Maven Central would put a POM there whose dependency Central cannot serve. A
  library on Central whose consumers must add JitPack is a decision, not an accident, and B-05
  should make it deliberately.
- The rejected alternative is to drop the JitPack-only dependency by vendoring or shading
  kotlin-telegram-bot. For a module in maintenance that is a great deal of work to avoid one line
  of documentation.
- Not covered: `:ktg`, whose ktgbotapi comes from Maven Central and resolves with no extra
  repository — which is one more thing that makes it the transport.

- AC: a project that copies the README's installation block verbatim, adds `:telegram`, and
  resolves it with a cache created for the run, succeeds.
- AC: the same block says why the extra repository is there, so nobody deletes it as noise.
- Anchors: `README.md`, `settings.gradle.kts`, `ci/consumer/settings.gradle.kts`.
