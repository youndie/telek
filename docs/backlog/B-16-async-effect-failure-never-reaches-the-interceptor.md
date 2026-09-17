---
id: B-16
title: "An async effect that throws reaches no interceptor, only the logger"
status: done
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

## Iteration 1 — 2026-09-17

Done. `EffectExecutorImpl.runAsync` logs **and rethrows** — the shape `FileStateStorage.save`
already had — and `Telek` catches it where the interceptors live and reports it.

- **The item's open question answered itself.** It asked what to pass as the input, guessing the
  input would be "long gone". It is not: the input whose transition launched the work is captured in
  the closure, and passing it makes the async path identical to the synchronous one, which was the
  whole complaint.
- **An existing test asserted the old behaviour by name** — `a throwing async handler is caught and
  logged and its work yields null instead of propagating`. That is a decision written down, not an
  accident, so it was rewritten rather than deleted and its comment says what changed and why.
- **The cancellation half is where this is easy to get wrong**, so it has its own test: a `Debounced`
  effect is cancelled as a matter of course, and if cancellation travelled the failure path, using
  debounce would look like a stream of errors. `CancellationException` is rethrown before the catch
  that reports, in both places.
- **The guard from [B-14](B-14-diagnostics-are-off-by-default.md) did not catch the documentation
  going stale, and that is its documented limit.** This change moved one diagnostic from "reported
  nowhere else" to "also reachable another way"; the count stayed at six, so the script stayed
  green while the table was wrong. Fixed by hand. A guard that reads the descriptions would have to
  understand them, which is why it counts instead.
