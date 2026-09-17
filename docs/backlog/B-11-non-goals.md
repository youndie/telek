---
id: B-11
title: "Write down the non-goals, because the unwritten ones get re-litigated"
status: done
priority: P2
size: XS
stage: stage-4-positioning
---

# B-11 — Write down the non-goals, because the unwritten ones get re-litigated

Several things this project has decided not to be are decided only in someone's head, or in a
build-file comment that answers a narrower question than the decision it came from —
`core/build.gradle.kts` explains why JS is excluded, as a note about ktgbotapi's target set rather
than as a scope decision. A non-goal that is not written down is re-proposed roughly once per
contributor, including by the author six months later, and each time it is argued from scratch.

- **The decision and its reason.** A short non-goals section in `README.md`, each item with the
  reason rather than the verdict. The four to start with:
  - **Its own Telegram client.** telek is a state machine with transports, not an API binding.
  - **JS and Apple targets.** ktgbotapi publishes neither; the target set is not telek's to widen.
  - **A third transport.** Two already double the work of every new input type —
    see [B-03](B-03-input-beyond-text.md) and [B-12](B-12-one-transport.md).
  - **Generalising beyond Telegram**, to "interactive systems" at large, until somebody asks. The
    core is transport-agnostic by construction; advertising that as a product is a different thing
    and nobody has requested it.
- The rejected alternative is a `CONTRIBUTING.md` or a docs page. Rejected because the audience for
  a non-goal is the person about to propose it, and they are reading the README.
- Not covered: whether private-chat-only belongs on this list. It is the rejected alternative inside
  [B-01](B-01-conversation-key.md), and if B-01 is ever refused, this is where it lands.

- AC: `README.md` has a non-goals section, and each entry states the reason, not just the refusal.
- Anchors: `README.md`, `core/build.gradle.kts`.

## Iteration 1 — 2026-09-17

Done, as a `What telek is not` section at the end of `README.md` — five entries, each with its
reason rather than its verdict, because the audience for a non-goal is the person about to propose
it and what they need is the argument.

Four came from the item; the fifth is [B-13](B-13-async-result-races-input.md), the implicit
async-cancellation proposal that was filed as dropped. It belongs here for exactly the reason this
item exists: a refusal that lives only in a closed backlog entry gets re-proposed by anyone who has
not read the backlog, which is everyone.

- **The `:telegram` entry carries B-12's sentence**, which that item left open when it could not
  write into a section that did not exist yet. B-12's second acceptance criterion is now satisfiable
  and is satisfied here.
- **Two factual claims in the section were checked rather than asserted.** "Nothing in `:core`
  imports a Telegram type" — grepped, and it is true. The JS exclusion's reason is stated inline
  (it would force an `expect`/`actual` for `Dispatchers.IO`) instead of pointing a reader at
  `core/build.gradle.kts`, which is where it was written and where nobody reading a README will go.
- **Found, not fixed, and worth a decision rather than an item:** the README's own tagline says
  telek is for "Telegram bots, wizard-flows, and other **interactive systems**", and the fourth
  non-goal declines to sell exactly that. The non-goal is worded to be true anyway — it distinguishes
  the *property* (the core is transport-agnostic, and that is real) from the *product* (adapters and
  a general runtime, which is not on offer). The tagline is positioning and belongs to the
  repository's owner, so it is left alone and named here instead of quietly rewritten.
