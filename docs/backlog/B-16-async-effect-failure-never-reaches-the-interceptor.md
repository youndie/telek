---
id: B-16
title: "An async effect that throws reaches no interceptor, only the logger"
status: wip
priority: P2
size: S
stage: stage-3-debts
---

# B-16 — An async effect that throws reaches no interceptor, only the logger

A synchronous [EffectHandler] that throws becomes an `EffectFailed`, and `Telek` hands that to
`TelekInterceptor.onError` (`core/src/commonMain/kotlin/io/github/youndie/telek/Telek.kt`). An
`AsyncEffectHandler` that throws does not: `EffectExecutorImpl.runAsync` catches it, logs it, and
returns `null` — and `null` there means "produced no event", which is the same thing a handler
says when it succeeded and had nothing to report.

So the two halves of the same mechanism report failure to two different places, and the async half
reports it to the one that is off by default (see
[B-14](B-14-diagnostics-are-off-by-default.md)). A bot with an interceptor wired up — the supported
way to see failures — sees synchronous ones and not asynchronous ones, with nothing saying why.

- **The decision and its reason.** Route an async handler's exception to
  `TelekInterceptor.onError` as well. The reason is that the asymmetry is invisible: the interceptor
  is the documented place failures arrive, so a bot that has one reasonably concludes it is seeing
  them all, and the gap shows up as a flow that silently stops advancing.
- The hard part is what to pass as the input: `onError(key, input, error)` takes the `Input` that
  caused the transition, and by the time an async handler fails, that input is long gone — the work
  was launched from a transition that has already completed. Either `onError` accepts `null` there
  (it already does) or the signature grows a variant that names the effect instead, which is the
  question this item has to answer.
- The rejected alternative is to say that async effects report through their `Event` and a bot
  should model failure as an event of its own. That is true, and it is also how the mechanism is
  meant to be used — but it does not cover a handler that throws *unexpectedly*, which is exactly
  the case an interceptor exists for.
- Not covered: changing what `null` from a handler means. It is a legitimate "nothing to report".

- AC: an `AsyncEffectHandler` that throws reaches `TelekInterceptor.onError`, and a test asserts it
  with the same shape as the existing synchronous one.
- AC: a handler that returns `null` normally still reaches no interceptor — the control that keeps
  the first criterion from being satisfied by reporting everything.
- Anchors: `core/src/commonMain/kotlin/io/github/youndie/telek/EffectExecutor.kt`,
  `core/src/commonMain/kotlin/io/github/youndie/telek/Telek.kt`,
  `core/src/commonMain/kotlin/io/github/youndie/telek/TelekInterceptor.kt`.
