---
id: B-11
title: "Write down the non-goals, because the unwritten ones get re-litigated"
status: open
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
