# Release notes

## 0.1.2

Two things: a **second Telegram transport** (ktgbotapi alongside kotlin-telegram-bot) and a
**Kotlin Multiplatform migration** — telek now publishes for JVM, linuxX64 and linuxArm64, so a bot
can ship as a native Linux binary.

Nothing changes for an existing JVM bot on `:telegram` except two small breaking changes in
`:persistence` and `:router`, both listed below. Gradle picks the right variant from module metadata
automatically.

---

### Highlights

- **`:ktg` — ktgbotapi transport.** Same API shape as `:telegram`: dispatchers, transitions,
  `sendMessage`/`editMessage`/`editMarkup`, the text and inline-keyboard DSLs all read identically.
  Pick whichever Telegram client you already use.
- **Multiplatform.** `:core`, `:ktg`, `:router`, `:router-ktg`, `:persistence` and `:testing` are
  now KMP, published for **JVM, linuxX64, linuxArm64**.
- **`:router` no longer needs `kotlin-reflect`** — closing a gap the 0.1.0 notes listed under
  "deliberately not done".
- Kotlin **2.4.10**.

---

### 🤖 The `:ktg` transport

```kotlin
implementation("ru.workinprogress.telek:ktg:<VERSION>")       // instead of :telegram
implementation("ru.workinprogress.telek:router-ktg:<VERSION>") // instead of :router-telegram
```

```kotlin
val bot = telegramBot("telegram token")
val contextSource = KtgContextSource(bot)

val telek = Telek(
    dispatchers = listOf(ExampleDispatcher()),
    effectExecutor = ktgEffectExecutor(contextSource),
)

bot.buildBehaviourWithLongPolling {
    connect(telek, contextSource)
}.join()
```

`connect()` is an extension on ktgbotapi's `BehaviourContext`. It subscribes `onText` and
`onDataCallbackQuery`, maps them to telek `Message`/`Callback` inputs keyed by `chatId`, and hands
the `TelegramBot` to the shared `KtgContextSource`.

Differences from `:telegram` worth knowing:

- **Callback queries are answered for you.** kotlin-telegram-bot does this itself; ktgbotapi
  doesn't, so `connect()` answers each handled callback query to stop the client's spinner. Pass
  `answerCallbackQueries = false` if a dispatcher answers with its own text or alert.
- **Failures arrive as exceptions, not result types.** ktgbotapi throws instead of returning a
  result, so the built-in handlers don't produce a failure result of their own — the exception
  reaches `EffectExecutorImpl`, which logs it and turns it into an `EffectFailed` that reaches
  `TelekInterceptor.onError`.
- **The bot exists up front.** ktgbotapi hands out its `TelegramBot` immediately, so
  `KtgContextSource(bot)` usually resolves at once; the deferred form (`KtgContextSource()` +
  `provide(bot)`) is still there for wiring built before the bot exists.
- Handler interfaces are `KtgEffectHandler` / `KtgAsyncEffectHandler` (taking ktgbotapi's
  `TelegramBot`); the effect marker is `KtgEffect`.
- `ContentMessage<TextContent>.asTelekInput()`, `DataCallbackQuery.asTelekInput()` and
  `Message.telekChatId` are public — a bot wiring its own updates (webhooks, a custom
  `FlowsUpdatesFilter`) can reuse the mapping without `connect()`.
- Callback queries with no attached message (inline-mode ones) can't be keyed by `chatId` and are
  ignored.

---

### 🧩 Multiplatform

| Module | JVM | linuxX64 / linuxArm64 |
|---|---|---|
| `:core`, `:router`, `:persistence`, `:testing` | ✅ | ✅ |
| `:ktg`, `:router-ktg` | ✅ | ✅ |
| `:telegram`, `:router-telegram` | ✅ | — |

`:telegram` and `:router-telegram` stay JVM-only because kotlin-telegram-bot is JVM-only. **If you
need a native binary, use the `:ktg` transport.**

The target set is capped by ktgbotapi, which publishes jvm / js / linux / mingw and **no Apple
targets**. JS is deliberately left out: it would force an `expect/actual` for `Dispatchers.IO` with
nothing asking for it.

Internals that changed to get there, none of them visible in the public API:

- **`telekIoDispatcher`** (`expect`/`actual`) replaces direct `Dispatchers.IO` use — that lives in
  coroutines' `concurrent` source set, not `common`. On JVM it *is* `Dispatchers.IO`; on Native it's
  a lazily-created 64-thread pool, mirroring what coroutines' own native `DefaultIoScheduler` does.
  (`Dispatchers.IO` is unreachable from a Native source set: an `internal val IO` member shadows the
  public `expect` extension.)
- **`ChatWorkers`** swapped `ConcurrentHashMap` for atomicfu's `SynchronizedObject`. atomicfu is used
  as a plain library — only locks, no `atomic()` fields — so its compiler plugin is deliberately not
  applied.
- `:persistence` moved from `java.io` to okio.

---

### ⚠️ Breaking changes

#### 1. `:persistence` uses okio paths

```diff
-stateStorageOf<YourState>(dir = File("./state"))
+stateStorageOf<YourState>(dir = "./state".toPath())   // okio.Path
```

`FileStateStorage` and `stateStorageOf` now take an `okio.Path` instead of a `java.io.File`, plus an
optional `fileSystem: FileSystem`. Behaviour is unchanged — still one JSON file per `chatId`, still
temp-file + atomic move.

Upside for tests: pass okio's `FakeFileSystem` to exercise a flow's persistence without touching
disk.

#### 2. Every `Route` must be `@Serializable`

Including routes with no properties at all:

```diff
 @RouteContext(scope = "example", action = "confirm")
+@Serializable
 class ExampleRouteConfirm : Route
```

`@RouteContext` is now a `@SerialInfo` annotation, so the serialization compiler plugin bakes it
into the route's generated `SerialDescriptor` and telek reads it from there instead of via
`KClass.annotations`. That's what removes the `kotlin-reflect` dependency and makes `:router` work
on every target. A route that isn't `@Serializable` fails at compile time, not at runtime.

---

### Under the hood

- CI runs `build` rather than `test ktlintCheck`: the KMP modules have no `test` task at all
  (`jvmTest` / `linuxX64Test` / `allTests`), so a bare `test` would have silently covered only the
  two JVM-only modules. Likewise `publishAllPublicationsToWipRepository` instead of
  `publishMavenPublicationToWipRepository` — the hand-rolled `maven` publication only exists on
  JVM-only modules, so the old task name would have published 2 of the 8 published modules.
- linuxX64 tests genuinely **run** in CI (ubuntu runner); linuxArm64 is compile-and-link only, since
  an x86-64 host can't execute those binaries.
- 187 tests on JVM; the common ones additionally run on linuxX64.

---

### Still deliberately not done

Carried over from 0.1.0, minus the `kotlin-reflect` item now closed:

- **`StateSlot<S>` / `:persistence` rework** — designed, not built. `:persistence` still assumes the
  FSM state *is* the entire user record, which doesn't fit a bot whose FSM state is one field of a
  larger profile.
- **`onEffectResults` only delivers the last result**, with no link between a result and the effect
  that produced it.
- **Command matching** doesn't understand `/cmd@botname` or `/cmd arg`; `"*"` is still a magic
  fallback string; `EffectRegistry` matches handlers by exact class, not subtype.
- **Ordering of an async result against a newer input** is not resolved: the per-chat actor
  guarantees they never run concurrently and run in arrival order, but arrival order is a network
  race. Dispatchers should guard on state in `transition(state, event)`.

---

## 0.1.0

Everything since the `0.0.1.x` line: correctness fixes, a new concurrency model, first-class async
effects, and a reshuffled module layout.

This is a **breaking release** — every incompatible change is listed below with a before/after.

---

### Highlights

- **Per-chat ordering is now guaranteed.** Inputs for one chat run strictly in arrival order, one
  at a time, via an internal per-chat actor — instead of racing `scope.launch` calls guarded by
  hand-rolled mutexes.
- **Async work is part of the model.** `AsyncEffectHandler` + `Event` let a dispatcher start
  network work without owning a `CoroutineScope` or calling `transitionGate.post` by hand — results
  re-enter the FSM as a second transition.
- **Effect handlers are `suspend`** and no longer block a dispatcher thread.
- **Three new modules**: `:testing` (test harness), `:router-telegram` (transport glue split out of
  `:router`), `:docs-samples` (compiled README samples).

---

### ⚠️ Breaking changes & migration

#### 1. `EffectHandler.handle` is now `suspend`

```diff
 interface EffectHandler<E : Effect> {
-    fun handle(context: ExecutionContext, effect: E): EffectResult
+    suspend fun handle(context: ExecutionContext, effect: E): EffectResult
 }
```

Same for `TelegramEffectHandler.handle(bot, effect)`. Handler bodies don't change — only the
signature. Handlers now run off the FSM's dispatcher, so blocking I/O inside them no longer stalls
it.

#### 2. `ExecutionContext` is supplied by a `TelegramContextSource`, not injected into `Telek`

`Telek.initIfNeeded(context)` is **gone**. The `Bot` instance is only available inside
`bot { dispatch { … } }`, so it's now resolved lazily through a `CompletableDeferred`-backed source
shared between `connect` and the executor.

```diff
+val contextSource = TelegramContextSource()
+
 val telek = Telek(
     dispatchers = …,
-    effectExecutor = telegramEffectExecutor(),
+    effectExecutor = telegramEffectExecutor(contextSource),
 )

 val bot = bot {
     dispatch {
-        connect(telek)
+        connect(telek, contextSource)
     }
 }
```

`telegramEffectExecutor` also gained optional `effectRegistry`, `failurePolicy` and `logger`
parameters (all defaulted).

#### 3. `EffectExecutor.execute` signature

Only affects custom `EffectExecutor` implementations — consumers using `telegramEffectExecutor()`
are unaffected.

```diff
-suspend fun execute(context: ExecutionContext, effects: List<Effect>): List<EffectResult>
+suspend fun execute(
+    effects: List<Effect>,
+    dispatchAsync: (key: Any?, work: suspend () -> Event?) -> Unit,
+): List<EffectResult>
```

The context is now the executor's own concern (`EffectExecutorImpl` takes a
`context: suspend () -> ExecutionContext`). Async effects produce no `EffectResult`; they are handed
to `dispatchAsync` along with their debounce key.

#### 4. `:router` no longer depends on `:telegram`

The single piece of Telegram glue — `RowBuilder.callback(name, route)` — moved to the new
`:router-telegram` module. The **package is unchanged** (`ru.workinprogress.telek.router`), so
imports stay as they are; only the build file changes:

```diff
 implementation("ru.workinprogress.telek:router:<VERSION>")
+implementation("ru.workinprogress.telek:router-telegram:<VERSION>")
```

`:router`'s core encode/decode can now be used with a non-Telegram transport.

#### 5. `:persistence` cleanups

- The duplicate `StateStorage` interface was removed (it was declared twice); `PersistableUserStateStoreImpl`
  now holds the interface type rather than the concrete class.
- `FileStateStorage.save` **throws on failure** instead of printing and swallowing. If you relied on
  save failures being silent, you now need a `runCatching`.

#### 6. `UserStateStore` implementations must not lock

`update` is now called exclusively through the per-chat actor, which already guarantees at most one
in-flight `update` per `chatId`, in order. Implementations that add their own per-chat mutex are
duplicating that guarantee — and the built-in ones had a real bug from doing so. `DefaultUserStateStore`
has no locking of its own any more. See the interface KDoc.

#### 7. `TransitionBuilder.build()` throws a typed error

Forgetting `newState = …` inside `transition { }` used to surface as a bare
`UninitializedPropertyAccessException`. It's now an `IllegalStateException` naming the mistake and
the number of effects already added. Behaviourally still a failure — only the exception type and
message changed.

---

### New features

#### Async effects & events

The headline change. A dispatcher can now start async work as an ordinary effect and handle its
result as a second transition, with no coroutine plumbing of its own:

```kotlin
data class FetchCatFactEffect(val chatId: Long) : Effect
data class CatFactLoaded(override val chatId: Long, val fact: String) : Event

class FetchCatFactEffectHandler(private val useCase: ExampleNetworkUseCase) :
    AsyncEffectHandler<FetchCatFactEffect> {
    override suspend fun handle(context: ExecutionContext, effect: FetchCatFactEffect): Event? =
        CatFactLoaded(effect.chatId, useCase().fact)
}

// registration
registry.registerAsync(FetchCatFactEffect::class, FetchCatFactEffectHandler(useCase))

// dispatcher
override fun transition(state: ExampleState, input: Input) = transition {
    newState = ExampleState.Loading
    add(FetchCatFactEffect(input.chatId))          // returns immediately
}

override fun transition(state: ExampleState, event: Event) = when {
    state is ExampleState.Loading && event is CatFactLoaded -> transition { … }
    else -> noTransition(state)
}
```

- `Event` — `interface Event { val chatId: Long }`. Deliberately **not** a subtype of `Input`:
  an input is an external message, an event is an internal completion, and events never start a
  flow (no `entry` equivalent).
- `AsyncEffectHandler<E>` — `suspend handle(context, effect): Event?`; returning `null` means "no
  follow-up transition".
- `EffectRegistry.registerAsync` / `getAsync` — an effect class is either sync or async, not both.
- `StateDispatcher.transition(state, event)` — defaults to `noTransition`, override where needed.
- Events route through the same per-chat worker as inputs, so ordering with regular input is
  automatic.
- `TelegramAsyncEffectHandler` mirrors `TelegramEffectHandler` for async effects needing the bot.

`transitionGate` was deliberately **kept**, not deprecated — it remains a legitimate escape hatch
for reacting to a synchronous `EffectResult` (e.g. capturing a sent message's id).

#### `Debounced` — opt-in async-effect deduplication

```kotlin
data class SearchEffect(val query: String) : Effect, Debounced {
    override val debounceKey = "search"
}
```

A newly dispatched async effect cancels any in-flight effect with the same key for that chat.
Opt-in by design: implicit auto-cancel-by-effect-class was rejected as too surprising a default.

#### `TelekLogger`

A no-op-by-default logging seam (`DEBUG`/`WARN`/`ERROR`), wired into `Telek`, `EffectExecutorImpl`
and the per-chat actor. Replaces the previous mix of `println` and `java.util.logging` under a
mislabelled logger name.

#### `EffectFailurePolicy`

`CONTINUE` (default, matches historical behaviour) or `FAIL_FAST` — stop the batch at the first
failed effect.

#### `:testing` module

Testing a dispatcher used to require hand-writing a `TransitionGate` double. That harness now ships:

```kotlin
testImplementation("ru.workinprogress.telek:testing:<VERSION>")
```

- `SyncTransitionGate<S>` — applies posted reducers synchronously against in-memory state.
- `RecordingEffectExecutor` — records effect batches instead of running them; can script per-effect
  results and simulate async effects.
- `TestExecutionContext`.

#### Backpressure & worker tuning

`Telek` gained three optional constructor parameters:

| Parameter | Default | Purpose |
|---|---|---|
| `chatWorkerIdleTimeout` | `15.minutes` | when an idle per-chat worker retires |
| `chatInboxCapacity` | `64` | bounded inbox; excess input is **dropped with a warning**, never blocks the caller |
| `logger` | `TelekLogger.NoOp` | where those warnings go |

---

### Bug fixes

| Area | Fix |
|---|---|
| `:core` | `DefaultUserStateStore.update` did not actually serialise updates — it read state *before* taking the lock, so concurrent updates could clobber each other. Fixed structurally by the per-chat actor; the store's own locking was removed. |
| `:core` | Message order within a chat was not guaranteed — every input was a separate `scope.launch`. Now strictly ordered per chat. |
| `:core` | Callback routing: `DefaultFindDispatcherStrategy` ignored `canHandleCallback`, so `StateDispatcherWithRoutes` was effectively dead in production. Routing order is now command → callback → state. |
| `:core` | Effect failures never reached `TelekInterceptor.onError` — a failed send looked like a successful transition. They're now reported. |
| `:telegram` | `EditMarkupEffectHandler` had an **inverted condition**: it returned success on failure and a result object on success. Errors are now `EffectFailed(exception)`. |
| `:persistence` | `FileStateStorage` silently lost state: no directory creation, non-atomic writes, swallowed exceptions. Now creates its directory, writes via temp-file + atomic move, and throws on failure. |
| `:persistence` | Mutex leak in `PersistableUserStateStoreImpl` — the per-chat mutex map grew without bound. Removed along with the locking itself. |
| `:example` | `./gradlew :example:run` never worked (`mainClass` was never configured). |

Three further concurrency bugs were found **empirically** while building the per-chat actor — by a
real-thread stress test, not by review — and are worth knowing about if you implement something
similar:

- a bounded channel with suspending `send()` lost items that raced worker retirement;
- `withTimeoutOrNull(idle) { inbox.receive() }` silently drops an element arriving exactly at the
  timeout — replaced with `select { onReceive; onTimeout }`;
- removing the worker from the registry *before* draining its inbox let a replacement worker run
  concurrently with the old one's tail, reordering work.

---

### Docs & tooling

- **README samples now compile.** Every code block in README.md has a counterpart in
  `:docs-samples`, so `./gradlew build` fails if a sample rots. It caught a genuine error on the
  first pass — the Router section had a `keyboard { … }` block that was never valid Kotlin.
- `ktlintCheck` runs in CI alongside tests.
- Publishing no longer resolves `version` to `null` outside CI.
- `:router`'s literal-dotted source directory (`ru.workinprogress.telek.router/`) became a normal
  nested path; `example`'s `Application.kt` moved into the `.example` package.
