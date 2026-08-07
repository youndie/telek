# telek

[![ktlint](https://img.shields.io/badge/ktlint%20code--style-%E2%9D%A4-FF4081.svg)](https://ktlint.github.io/)
[![kotlin](https://img.shields.io/badge/Kotlin-2.4.0-blue?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![telek core](https://reposilite.kotlin.website/api/badge/latest/snapshots/ru/workinprogress/telek/core?name=snapshots&color=40c14a&prefix=v)](https://reposilite.kotlin.website/#/snapshots/ru/workinprogress/telek)
[![API Docs](https://img.shields.io/badge/docs-Dokka-blue?logoColor=white)](https://youndie.github.io/telek/)

**type-safe kotlin toolkit** for building **Telegram bots**, **wizard-flows**, and other **interactive systems** powered
by **FSM**

> 🧩 state + input → newState + effects

### 📦 Installation

Add the Reposilite snapshot repository and *telek* dependencies:

```kotlin
repositories {
    mavenCentral()
    maven {
        name = "reposiliteRepositorySnapshots"
        url = uri("https://reposilite.kotlin.website/snapshots")
    }
}

dependencies {
    implementation("ru.workinprogress.telek:core:<VERSION>")

    // pick one transport:
    implementation("ru.workinprogress.telek:telegram:<VERSION>") // kotlin-telegram-bot
    // implementation("ru.workinprogress.telek:ktg:<VERSION>")   // ktgbotapi
}
```
The core module contains the FSM engine, transitions, and effect system.
The telegram module provides integration
with [kotlin-telegram-bot](https://github.com/kotlin-telegram-bot/kotlin-telegram-bot), and the
`ktg` module the same integration
on [ktgbotapi](https://github.com/InsanusMokrassar/TelegramBotAPI) — same API shape, pick whichever
Telegram client you already use (see [Using ktgbotapi instead](#-using-ktgbotapi-instead)).

**Multiplatform.** `core`, `ktg`, `router`, `router-ktg`, `persistence` and `testing` are Kotlin
Multiplatform, published for **JVM, linuxX64 and linuxArm64** — so a bot can also ship as a native
Linux binary. `telegram` and `router-telegram` are JVM-only, because kotlin-telegram-bot is. A plain
JVM Gradle project resolves the right variant automatically from Gradle module metadata; nothing
changes for JVM consumers.


### 💬 Usage with Telegram bot

*telek* integrates seamlessly with [kotlin-telegram-bot](https://github.com/kotlin-telegram-bot/kotlin-telegram-bot)  
Each **StateDispatcher** describes one conversational flow — for example, a multistep wizard

Below is a simple dispatcher handling a confirmation dialog:

```kotlin
// Dispatcher that manages the conversation flow (FSM) for the 'example' command
class ExampleDispatcher : StateDispatcher<ExampleState>() {
    // The command that starts this dispatcher flow
    override val startCommand = "example"
    // The associated state class for this flow
    override val stateClass = ExampleState::class

    // Handles finite-state transitions based on current state and input
    override fun transition(
        state: ExampleState,
        input: Input,
    ): TransitionResult<ExampleState> =
        when (state) {
            // If waiting for a string, and receive a message input from user
            is ExampleState.WaitingString if (input is Message) -> {
                transition {
                    // Move to Confirming state, keep number, save input string
                    newState = ExampleState.Confirming(
                        number = state.number,
                        string = input.text,
                    )
                    // Send confirmation message with inline keyboard (Confirm/Cancel)
                    sendMessage(
                        input.chatId,
                        message = {
                            row {
                                text("Confirm?")
                            }
                        },
                        keyboard = {
                            row {
                                callback(text = "Confirm", data = "example_confirm")
                                callback(text = "Cancel", data = "example_cancel")
                            }
                        },
                    )
                }
            }
            // If in Confirming state and receive a callback from the inline keyboard
            is ExampleState.Confirming if (input is Callback) -> {
                transition {
                    // Move to Done state
                    newState = ExampleState.Done
                    // Remove inline keyboard from message
                    editMarkup(input.chatId, input.messageId, null)
                    // Respond with confirmation or cancellation based on callback data
                    if (input.data.contains("example_confirm")) {
                        sendMessage(input.chatId, "confirmed")
                    } else {
                        sendMessage(input.chatId, "canceled")
                    }
                }
            }
            // For all other cases, no state transition
            else -> noTransition(state)
        }
}
```

This example shows how *telek* lets you:

* 🧩 Define a finite-state flow per user
* 💬 Send messages and inline keyboards declaratively
* 🔁 Handle message and callback inputs as FSM transitions
* ✨ Keep logic pure and testable — no Telegram API calls inside your states


### 🚀 Initialization

below is a minimal setup example using a parent coroutine scope, and interceptors.

```kotlin
val contextSource = TelegramContextSource()

val telek = Telek(
    dispatchers = listOf(ExampleDispatcher()),
    effectExecutor = telegramEffectExecutor(contextSource),
)

bot {
    token = "telegram token"

    dispatch { connect(telek, contextSource) }
}
```

`TelegramContextSource` is what lets the effect executor reach the `Bot` instance: `bot { }` only
hands one out inside a `dispatch { }` handler, so the executor and `connect()` share one source that
resolves lazily on the first update. Both `EffectExecutor.execute` and every `EffectHandler.handle`
are `suspend` — handlers run on `Dispatchers.IO`, off whatever dispatcher your chats' transitions run
on, so a slow Telegram API call for one chat never blocks another chat's turn.


### 🤖 Using ktgbotapi instead

The `ktg` module is the same integration built
on [ktgbotapi](https://github.com/InsanusMokrassar/TelegramBotAPI) instead of kotlin-telegram-bot.
Everything above — dispatchers, transitions, `sendMessage` / `editMessage` / `editMarkup`, the text
and inline-keyboard DSLs — reads identically; only the imports change
(`ru.workinprogress.telek.telegram.*` → `ru.workinprogress.telek.ktg.*`) and the types they produce
are ktgbotapi's (`InlineKeyboardMarkup`, `TelegramBot`).

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

`connect()` is an extension on ktgbotapi's `BehaviourContext`: it subscribes `onText` and
`onDataCallbackQuery`, maps them to telek `Message` / `Callback` inputs keyed by `chatId`, and hands
the `TelegramBot` to the shared `KtgContextSource`. It answers each handled callback query by default
so Telegram stops the client's spinner — pass `answerCallbackQueries = false` if a dispatcher answers
with its own text or alert. Callback queries with no message attached (inline-mode ones) can't be
keyed by `chatId` and are ignored.

Unlike kotlin-telegram-bot, ktgbotapi hands out its `TelegramBot` up front, so `KtgContextSource(bot)`
usually resolves immediately; the deferred form (`KtgContextSource()` + `provide(bot)`) is still there
for wiring built before the bot exists.

Differences worth knowing:

* Effect handler interfaces are `KtgEffectHandler` / `KtgAsyncEffectHandler`, taking ktgbotapi's
  `TelegramBot` rather than kotlin-telegram-bot's `Bot`; the marker interface for effects is
  `KtgEffect`.
* ktgbotapi reports API failures by throwing rather than returning a result type, so the built-in
  handlers don't return a failure result of their own — the exception reaches `EffectExecutorImpl`,
  which logs it and turns it into an `EffectFailed` that reaches `TelekInterceptor.onError`.
* `ContentMessage<TextContent>.asTelekInput()`, `DataCallbackQuery.asTelekInput()` and
  `Message.telekChatId` are public, so a bot wiring its own updates (webhooks, a custom
  `FlowsUpdatesFilter`) can reuse the mapping without going through `connect()`.

The router equivalent is `router-ktg` — same `RowBuilder.callback(name, route)` extension, over
`ru.workinprogress.telek.ktg.RowBuilder`.


### ⚡ Defining a Custom Effect

*telek* lets you extend its behavior with **custom effects** —  
your own side-effects that will be executed during a transition.

Below is an example of creating a custom effect that deletes a Telegram message.

```kotlin
// Define your custom effect
data class CustomEffect(
    val chatId: Long,
    val messageId: Long,
) : TelegramEffect

// Implement its handler
class CustomEffectHandler : TelegramEffectHandler<CustomEffect> {
    override suspend fun handle(
        bot: Bot,
        effect: CustomEffect,
    ): EffectResult =
        bot
            .deleteMessage(ChatId.fromId(effect.chatId), effect.messageId)
            .fold({ EffectSuccess }, { error -> EffectFailed(IllegalStateException(error.toString())) })
}

// DSL extension for transitions
fun <S : State> TransitionBuilder<S>.customEffect(
    chatId: Long,
    messageId: Long,
) {
    add(CustomEffect(chatId, messageId))
}
```

Now register it in your EffectRegistry:

```kotlin
val effectRegistry =
    defaultEffectRegistry().apply {
        register(CustomEffect::class, CustomEffectHandler())
    }

val effectExecutor = telegramEffectExecutor(contextSource, effectRegistry)
```

And use it inside a transition:

```kotlin 
transition {
    customEffect(input.chatId, input.messageId)
}
```

This mechanism allows you to:

* 🧩 Add new side-effects without modifying *telek* core
* 🔌 Integrate any external actions (e.g., analytics, notifications, cleanup)
* 🧠 Keep your state logic pure while handling Telegram I/O declaratively


### ⏳ Async effects & Events

A regular effect runs as part of the transition that created it — fine for sending a message, wrong
for a network call: you don't want to block the chat's next input on it. An **async effect** runs
independently and reports back later as an **`Event`**, which re-enters the FSM through its own
`transition(state, event)` overload — no manual `CoroutineScope`, no posting results back by hand.

```kotlin
// The effect just carries what the handler needs
data class FetchCatFactEffect(val chatId: Long) : Effect

// ...and what comes back, once it's done
data class CatFactLoaded(override val chatId: Long, val fact: String) : Event
data class CatFactLoadFailed(override val chatId: Long, val errorMessage: String) : Event

// AsyncEffectHandler, not EffectHandler — returns an Event instead of an EffectResult
class FetchCatFactEffectHandler(
    private val networkUseCase: FetchCatFactUseCase,
) : AsyncEffectHandler<FetchCatFactEffect> {
    override suspend fun handle(context: ExecutionContext, effect: FetchCatFactEffect): Event =
        networkUseCase()
            .fold(
                { fact -> CatFactLoaded(effect.chatId, fact.text) },
                { error -> CatFactLoadFailed(effect.chatId, error.message ?: "Unknown error") },
            )
}
```

Register it with `registerAsync` instead of `register`, then add the effect from a transition like
any other:

```kotlin
val effectRegistry = defaultEffectRegistry().apply {
    registerAsync(FetchCatFactEffect::class, FetchCatFactEffectHandler(useCase))
}

// inside a transition
transition {
    newState = MyState.Loading
    sendMessage(input.chatId, "Loading...")
    add(FetchCatFactEffect(chatId = input.chatId))   // fire-and-forget from here on
}
```

And handle the result with the `Event` overload of `transition` — there's no `entry` equivalent for
events, since an event never starts a flow, only continues one:

```kotlin
override fun transition(state: MyState, event: Event): TransitionResult<MyState> =
    when {
        state is MyState.Loading && event is CatFactLoaded ->
            transition { newState = MyState.Done(event.fact) }

        state is MyState.Loading && event is CatFactLoadFailed ->
            transition { newState = MyState.Error(event.errorMessage) }

        else -> noTransition(state)
    }
```

Notes:

* The async effect's coroutine is tied to that chat's lifecycle — if the chat goes idle, it's
  cancelled along with everything else for that chat.
* A dispatcher whose async handler doesn't need `Bot` access can implement `AsyncEffectHandler`
  directly, as above. One that does needs `Bot` should implement `TelegramAsyncEffectHandler`
  instead — same relationship as `EffectHandler` / `TelegramEffectHandler`.
* See `:example`'s `ExampleDispatcher` for the full pattern in context (fetching a cat fact while
  showing a "Loading..." message).

**Debounce (opt-in).** By default two rapid inputs that each start the same async effect run
concurrently — whichever resolves last wins. If you want latest-wins semantics instead (typical for
"search as you type"), implement the `Debounced` marker on the effect:

```kotlin
data class SearchProductsEffect(val chatId: Long, val query: String) : Effect, Debounced {
    override val debounceKey: Any get() = "search" // per-chat: same key cancels the previous in-flight search
}
```

When a `Debounced` async effect is dispatched, the previous still-running handler for the same
`debounceKey` **in the same chat** is cancelled before the new one starts. Different keys don't
interfere, and effects without the marker are never auto-cancelled.


### 📦 Optional modules

Add optional modules if you need persistence or compact callback routing:

```kotlin
dependencies {
    // ... core + telegram as shown above
    implementation("ru.workinprogress.telek:persistence:<VERSION>")
    implementation("ru.workinprogress.telek:router:<VERSION>")

    // only if you're building inline keyboards with typed routes (RowBuilder.callback(name, route)) —
    // pick the one matching your transport
    implementation("ru.workinprogress.telek:router-telegram:<VERSION>")
    // implementation("ru.workinprogress.telek:router-ktg:<VERSION>")
}
```

`:router` itself doesn't depend on any transport — the route encode/decode logic (`Route`,
`RouteRegistry`, `@RouteContext`) is transport-agnostic. `:router-telegram` and `:router-ktg` add the
one bit of glue that needs a transport: the `RowBuilder.callback(name, route)` extension used below.

### 💾 Persistence module

Persist user states between bot restarts using the `persistence` module. It provides a simple JSON file storage and a `UserStateStore` implementation.

Key components:
- `FileStateStorage<T : State>` — saves/loads states as JSON files, one per `chatId`
- `stateStorageOf<T>()` — convenience factory for `FileStateStorage`
- `PersistableUserStateStoreImpl<T : State>` — drop‑in replacement for the default in‑memory store

File access goes through [okio](https://square.github.io/okio/) rather than `java.io`, so paths are
`okio.Path` and the module works on native targets too. Both take an optional `fileSystem` — pass
okio's `FakeFileSystem` to test a flow's persistence without touching the disk.

Usage:

```kotlin
// Suppose your flow uses states of type YourState : State
val userStateStore = PersistableUserStateStoreImpl<YourState>(
    stateStorageOf(dir = "./state".toPath()) // files like ./state/<chatId>.json
)

val telek = Telek(
    userStateStore = userStateStore,
    dispatchers = listOf(ExampleDispatcher()),
    effectExecutor = telegramEffectExecutor(contextSource),
)
```

Notes:
- JSON serialization is powered by `kotlinx.serialization` with `classDiscriminator = "state_type"` and `ignoreUnknownKeys = true`.
- When a transition returns a `FinalState`, the storage entry is automatically deleted by `PersistableUserStateStoreImpl`.

### 🧭 Router module

Create compact, type‑safe callback data for inline keyboards and decode them easily.

Define routes:

```kotlin
@RouteContext(scope = "example", action = "select")
@Serializable
class ExampleRouteSelect(val number: Int) : Route

@RouteContext(scope = "example", action = "confirm")
@Serializable
class ExampleRouteConfirm : Route

@RouteContext(scope = "example", action = "cancel")
@Serializable
class ExampleRouteCancel : Route
```

Build a registry and use helpers:

```kotlin
val registry = routes {
    register<ExampleRouteSelect>()
    register<ExampleRouteConfirm>()
    register<ExampleRouteCancel>()
}

// Build inline keyboard with typed routes
sendMessage(
    chatId = input.chatId,
    message = { row { text("Choose:") } },
    keyboard = {
        row {
            // `callback(name, route)` comes from the router-telegram (or router-ktg) module
            callback(name = "Confirm", route = ExampleRouteConfirm())
            callback(name = "Cancel", route = ExampleRouteCancel())
        }
    },
)

// Handle callbacks in a dispatcher
when (input) {
    is Callback -> {
        when {
            input.isRouteOf<ExampleRouteConfirm>(registry) -> { /* handle confirm */ }
            input.isRouteOf<ExampleRouteCancel>(registry) -> { /* handle cancel */ }
            else -> input.tryDecode<ExampleRouteSelect>(registry)?.let { route ->
                val n = route.number
                // handle selection of `n`
            }
        }
    }
    else -> { /* other inputs */ }
}
```

How it works:
- Each `Route` must be annotated with `@RouteContext(scope, action)` **and** with `@Serializable` — including routes with no fields at all.
- `@RouteContext` is a `@SerialInfo` annotation, so the serialization compiler plugin bakes it into the route's generated `SerialDescriptor` and telek reads it from there. That's what lets `:router` work on every target: `KClass.annotations` needs JVM-only reflection, and `:router` no longer depends on `kotlin-reflect` at all.
- The encoder produces strings like `scope:action:key1_val1_key2_val2` using `kotlinx.serialization` properties format.
- `routes { register<T>() }` adds decoders per route type, enabling `isRouteOf<T>()` and `tryDecode<T>()` on `Callback`.
