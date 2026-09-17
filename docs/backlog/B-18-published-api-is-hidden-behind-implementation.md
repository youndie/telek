---
id: B-18
title: "Published modules hide, behind implementation, the types their own API names"
status: open
priority: P1
size: S
stage: stage-1-release
---

# B-18 — Published modules hide, behind `implementation`, the types their own API names

Every module declares its dependencies with `implementation`, so the published `apiElements` of
most of them contain nothing but `kotlin-stdlib`. Three of those modules have public API that names
a type from a dependency, and a consumer compiling against them cannot see it:

| Module | Its API names | `jvmApiElements` carries |
|---|---|---|
| `core` | `Telek(scope: CoroutineScope, …)` | `kotlin-stdlib` only |
| `router` | `Callback.isRouteOf(…)` — `Callback` is a `core` type; and every `Route` a consumer writes must be `@Serializable` | `kotlin-stdlib` only |
| `persistence` | every `State` a consumer stores must be `@Serializable` | `okio`, `kotlin-stdlib` |

`testing` does it correctly — its `apiElements` carries `core` — which is what says this is an
oversight rather than a policy.

**The runtime is fine.** `jvmRuntimeElements` carries the full set, checked against the published
metadata of `router-jvm` before writing this: nothing is missing at run time, and nothing
`NoClassDefFoundError`s. The cost is entirely at compile time, and it lands on the consumer.

Found by [B-17](B-17-consumer-acceptance-from-outside.md) on its first run: the consumer could not
resolve `kotlinx.serialization.Serializable` while writing the `@Serializable` annotations that
`:router` and `:persistence` *require* of it. The others are visible in the same metadata but were
not hit, because that consumer does not construct a `Telek`.

- **The decision and its reason.** Promote to `api` exactly what a module's own public API names:
  `kotlinx-coroutines-core` in `core`, `core` and `kotlinx-serialization-core` in `router`,
  `kotlinx-serialization-core` in `persistence`. The reason is that the alternative — every
  consumer repeating those coordinates — makes telek's dependency versions the consumer's problem
  for no benefit, and the failure is a compile error with no hint of which module was at fault.
- Note `kotlinx-serialization-properties` (the format `:router` encodes with) should stay
  `implementation`: a consumer never names it. It is `-core` that carries `@Serializable`.
- The rejected alternative is to document "also add these" in the README. That makes every consumer
  carry a version telek chose, and drifts the moment telek changes one.
- Not covered: whether `:router` should depend on `:core` at all rather than the other way round.
  That is a layering question, not this one.

- AC: the [B-17](B-17-consumer-acceptance-from-outside.md) consumer compiles with **no** direct
  dependency on kotlinx-serialization or kotlinx-coroutines, and constructs a `Telek` with its own
  `CoroutineScope`.
- AC: the published `jvmApiElements` of `core`, `router` and `persistence` name what the table above
  says they should — read off the metadata, not off the build file.
- Anchors: `core/build.gradle.kts`, `router/build.gradle.kts`, `persistence/build.gradle.kts`,
  `testing/build.gradle.kts`.
