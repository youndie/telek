package io.github.youndie.telek

import io.github.youndie.telek.support.TestEffect
import io.github.youndie.telek.support.TestExecutionContext
import io.github.youndie.telek.support.TestState
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

/** One result per sent message, carrying the id Telegram gave it. */
private data class SentMessage(
    val messageId: Long,
) : EffectResult

/**
 * B-09's acceptance. The dispatcher sends two messages and needs the id of the *first* — which,
 * before outcomes carried their effect, meant assuming the results line up positionally with the
 * effects. The tests below are written the way a bot would have to write it now, and the second one
 * is the whole argument: inserting a third effect changes every position and changes nothing here.
 */
private class TwoMessagesDispatcher : StateDispatcher<TestState>() {
    override val startCommand = "two"
    override val stateClass = TestState::class

    val greeting = TestEffect("send-greeting")
    val question = TestEffect("send-question")

    var greetingId: Long? = null
    var questionId: Long? = null

    override fun transition(
        state: TestState,
        input: Input,
    ): TransitionResult<TestState> = noTransition(state)

    override fun onEffectResults(
        state: State,
        outcomes: List<EffectOutcome>,
    ) {
        greetingId = outcomes.idOf(greeting)
        questionId = outcomes.idOf(question)
    }

    private fun List<EffectOutcome>.idOf(effect: Effect): Long? =
        firstOrNull { it.effect == effect }?.let { (it.result as? SentMessage)?.messageId }
}

class EffectOutcomeTest {
    private fun executor(ids: Map<Effect, Long>) =
        EffectExecutorImpl(
            effectRegistry =
                EffectRegistry().apply {
                    register(
                        TestEffect::class,
                        object : EffectHandler<TestEffect> {
                            override suspend fun handle(
                                context: ExecutionContext,
                                effect: TestEffect,
                            ): EffectResult = ids[effect]?.let { SentMessage(it) } ?: EffectSuccess
                        },
                    )
                },
            context = { TestExecutionContext },
        )

    @Test
    fun `a dispatcher tells two sent messages apart by the effect each result came from`() =
        runTest {
            val dispatcher = TwoMessagesDispatcher()
            val ids = mapOf<Effect, Long>(dispatcher.greeting to 100L, dispatcher.question to 200L)

            val outcomes = executor(ids).execute(listOf(dispatcher.greeting, dispatcher.question)) { _, _ -> }
            dispatcher.onEffectResults(TestState.Waiting(0), outcomes)

            assertEquals(100L, dispatcher.greetingId)
            assertEquals(200L, dispatcher.questionId)
        }

    @Test
    fun `inserting a third effect between them changes nothing`() =
        runTest {
            val dispatcher = TwoMessagesDispatcher()
            val filler = TestEffect("something-else")
            val ids = mapOf<Effect, Long>(dispatcher.greeting to 100L, dispatcher.question to 200L, filler to 999L)

            val outcomes =
                executor(ids).execute(listOf(dispatcher.greeting, filler, dispatcher.question)) { _, _ -> }
            dispatcher.onEffectResults(TestState.Waiting(0), outcomes)

            // A positional reading would have taken 999 for the question here, silently.
            assertEquals(100L, dispatcher.greetingId)
            assertEquals(200L, dispatcher.questionId)
        }

    @Test
    fun `an outcome names the effect it came from even when nothing interesting happened`() =
        runTest {
            val effect = TestEffect("plain")

            val outcomes = executor(emptyMap()).execute(listOf(effect)) { _, _ -> }

            assertEquals(EffectOutcome(effect, EffectSuccess), outcomes.single())
        }
}
