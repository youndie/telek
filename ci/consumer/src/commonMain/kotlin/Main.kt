// A bot-shaped use of telek, written against the published artefacts only.
//
// Everything here is API this repository added today, so a publication that is missing a module, a
// target, or a class fails to compile rather than passing quietly.

import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import io.github.youndie.telek.Callback
import io.github.youndie.telek.Command
import io.github.youndie.telek.ConversationKey
import io.github.youndie.telek.Effect
import io.github.youndie.telek.EffectOutcome
import io.github.youndie.telek.FileRef
import io.github.youndie.telek.FinalState
import io.github.youndie.telek.Input
import io.github.youndie.telek.Keying
import io.github.youndie.telek.Message
import io.github.youndie.telek.Photo
import io.github.youndie.telek.State
import io.github.youndie.telek.StateDispatcher
import io.github.youndie.telek.Telek
import io.github.youndie.telek.TransitionResult
import io.github.youndie.telek.asCommand
import io.github.youndie.telek.noTransition
import io.github.youndie.telek.ktg.KtgContextSource
import io.github.youndie.telek.ktg.connect
import io.github.youndie.telek.persistence.stateStorageOf
import io.github.youndie.telek.router.Route
import io.github.youndie.telek.router.RouteContext
import io.github.youndie.telek.router.isRouteOf
import io.github.youndie.telek.router.routes
import io.github.youndie.telek.testing.RecordingEffectExecutor
import io.github.youndie.telek.transition
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.Serializable
import okio.Path.Companion.toPath
import io.github.youndie.telek.TextEntityType
import io.github.youndie.telek.entities
import io.github.youndie.telek.message

@RouteContext(scope = "consumer", action = "confirm")
@Serializable
class ConsumerConfirm : Route

private val consumerRoutes =
    routes {
        register<ConsumerConfirm>()
    }

@Serializable
sealed interface PassportState : State {
    @Serializable
    data object AwaitingScan : PassportState

    @Serializable
    data class Confirming(
        val fileId: String,
    ) : PassportState

    @Serializable
    data object Done : PassportState, FinalState
}

private data class Reply(
    val chatId: Long,
    val text: String,
) : Effect

class PassportDispatcher : StateDispatcher<PassportState>() {
    override val startCommand: String = "passport"
    override val stateClass = PassportState::class

    // B-04: the argument a command can now carry.
    var token: String? = null

    // B-09: which effect a result came from.
    var lastEffect: Effect? = null

    override fun entry(input: Input): TransitionResult<PassportState>? {
        val command = (input as? Message)?.asCommand() ?: return null
        if (command.name != startCommand) return null
        token = command.argument
        return transition {
            newState = PassportState.AwaitingScan
            add(Reply(input.chatId, "Send a photo of your passport."))
        }
    }

    override fun transition(
        state: PassportState,
        input: Input,
    ): TransitionResult<PassportState> =
        when {
            // B-03: an input that is not text and not a callback.
            state is PassportState.AwaitingScan && input is Photo ->
                transition {
                    newState = PassportState.Confirming(input.file.fileId)
                    add(Reply(input.chatId, "Got it. Confirm?"))
                }

            state is PassportState.Confirming && input is Callback && input.isRouteOf<ConsumerConfirm>(consumerRoutes) ->
                transition {
                    newState = PassportState.Done
                    add(Reply(input.chatId, "Filed."))
                }

            else -> noTransition(state)
        }

    override fun onEffectResults(
        state: State,
        outcomes: List<EffectOutcome>,
    ) {
        lastEffect = outcomes.lastOrNull()?.effect
    }
}

fun main() {
    val dispatcher = PassportDispatcher()

    // B-01: the key a conversation is filed under, and the two shapes of it.
    val alice: ConversationKey = ConversationKey.chatAndUser(chatId = -100, userId = 11)
    val group: ConversationKey = ConversationKey.chat(chatId = -100)
    check(alice != group) { "a per-user key and a chat key must be different keys" }
    check(alice.storageId == "-100.11") { "unexpected storageId: ${alice.storageId}" }
    check(Keying.PerUserInChat.key(-100, 11) == alice) { "Keying disagrees with ConversationKey" }

    // B-04: /cmd@botname with an argument.
    val command: Command? = Message(chatId = -100, text = "/passport@mybot ABC-123").asCommand()
    check(command?.name == "passport" && command.argument == "ABC-123") { "command parsing: $command" }

    // B-03 + the flow, as a pure function.
    val started = dispatcher.entry(Message(-100, "/passport ABC-123"))
    check(started?.newState == PassportState.AwaitingScan) { "entry did not start the flow" }
    check(dispatcher.token == "ABC-123") { "the command argument did not reach the dispatcher" }

    val scanned =
        dispatcher.transition(
            PassportState.AwaitingScan,
            Photo(chatId = -100, messageId = 7, file = FileRef("scan-1")),
        )
    check(scanned.newState == PassportState.Confirming("scan-1")) { "a photo did not advance the flow" }

    // B-09: an outcome names its effect.
    dispatcher.onEffectResults(scanned.newState, scanned.effects.map { EffectOutcome(it, io.github.youndie.telek.EffectSuccess) })
    check(dispatcher.lastEffect != null) { "an outcome did not carry its effect" }

    // B-22: the body is a document, and a name full of markup characters stays a name.
    val hostile = "a_b*c[d`e"
    val body =
        message {
            bold("Passport")
            br()
            text(hostile)
            expandableBlockquote { code("ORD-4711") }
        }
    check(body.plain == "Passport\n" + hostile + "ORD-4711") { "plain projection: ${body.plain}" }
    check(body.entities().map { it.type } == listOf(TextEntityType.Bold, TextEntityType.ExpandableBlockquote, TextEntityType.Code)) {
        "entities: ${body.entities()}"
    }
    check(body.entities().first { it.type == TextEntityType.Bold }.length == 8) { "bold range is wrong" }

    // :persistence resolves and constructs on every target.
    stateStorageOf<PassportState>(dir = "./state".toPath())

    // B-18's other half: `Telek`'s constructor takes a CoroutineScope, and until :core exposed
    // kotlinx-coroutines as `api` a consumer could not name the type it was being asked for.
    // Nothing here declares a coroutines dependency -- if this line compiles, :core carries it.
    val telek =
        Telek(
            scope = CoroutineScope(Dispatchers.Default),
            dispatchers = listOf(dispatcher),
            effectExecutor = RecordingEffectExecutor(),
        )
    telek.onInput(alice, Message(-100, "/passport ABC-123"))

    println("telek consumer: resolved and exercised the published artefacts")
}

/**
 * The wiring, compiled but not run.
 *
 * B-20: `connect()` is the seam that decides the [ConversationKey], and until this existed the
 * consumer exercised only [Telek.onInput] — so telek's own acceptance had the blind spot it had
 * just found in somebody else's. Running it needs a bot and a token; compiling it is what catches a
 * signature change, which is the failure this is for.
 */
@Suppress("unused")
private fun BehaviourContext.wiring(
    telek: Telek,
    contextSource: KtgContextSource,
) {
    connect(telek, contextSource, Keying.PerUserInChat)
    connect(telek, contextSource, Keying.PerChat, answerCallbackQueries = false)
}
