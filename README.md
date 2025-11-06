# telek

![ktlint](https://img.shields.io/badge/ktlint%20code--style-%E2%9D%A4-FF4081.svg)
![kotlin](https://camo.githubusercontent.com/3f686300866ac5df37a4223daffb3acfbf9b580619af1411286a0d55e8f96307/68747470733a2f2f696d672e736869656c64732e696f2f62616467652f6b6f746c696e2d322e322e32312d626c75652e7376673f6c6f676f3d6b6f746c696e)
![telek core](https://reposilite.kotlin.website/api/badge/latest/snapshots/ru/workinprogress/telek/core?name=Snapshots&color=40c14a&prefix=v)

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
    implementation("ru.workinprogress.telek:telegram:<VERSION>")
}
```
The core module contains the FSM engine, transitions, and effect system.
The telegram module provides integration
with [kotlin-telegram-bot](https://github.com/kotlin-telegram-bot/kotlin-telegram-bot)


### 💬 Usage with Telegram bot

*telek* integrates seamlessly with [kotlin-telegram-bot](https://github.com/kotlin-telegram-bot/kotlin-telegram-bot)  
Each **StateDispatcher** describes one conversational flow — for example, a multistep wizard

Below is a simple dispatcher handling a confirmation dialog:

```kotlin 
class ExampleDispatcher(
    effectExecutor: EffectExecutor,
) : StateDispatcher<ExampleState>(effectExecutor) {
    override val startCommand = "example"
    override val stateClass = ExampleState::class

    override fun transition(
        state: ExampleState,
        input: Input,
    ): TransitionResult<ExampleState> =
        when (state) {
            is ExampleState.WaitingString if (input is Input.Message) -> {
                transition {
                    newState =
                        ExampleState.Confirming(
                            number = state.number,
                            string = input.text,
                        )

                    sendMessage(
                        input.chatId,
                        message = {
                            row {
                                text("CONFIRM?")
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

            is ExampleState.Confirming if (input is Input.Callback) -> {
                transition {
                    newState = ExampleState.Done

                    editMarkup(input.chatId, input.messageId, null)

                    if (input.data.contains("example_confirm")) {
                        sendMessage(input.chatId, "confirmed")
                    } else {
                        sendMessage(input.chatId, "canceled")
                    }
                }
            }

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
bot {
    token = "telegram token"

    val effectExecutor = telegramEffectExecutor()

    val telek = Telek(
        scope = CoroutineScope(parentScope.coroutineContext + Dispatchers.Default),
        dispatchers = listOf(ExampleDispatcher(effectExecutor)),
        stateProvider = StateProvider { EmptyState },
        interceptors = listOf(LoggingInterceptor),
    )

    dispatch { connect(telek) }
}
```


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
    ) {
        bot.deleteMessage(ChatId.fromId(effect.chatId), effect.messageId)
    }
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

val effectExecutor = EffectExecutorImpl(effectRegistry)
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
