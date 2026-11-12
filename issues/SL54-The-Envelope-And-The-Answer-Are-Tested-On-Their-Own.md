# SL54 — The Envelope And The Answer Are Tested On Their Own

```json
{
  "$schema": "issue-1.json",
  "id": "SL54",
  "title": "The Envelope And The Answer Are Tested On Their Own",
  "state": "Estimated",
  "size": "S",
  "priority": "Medium",
  "rank": 5,
  "blockedBy": [],
  "covers": "Unit tests of `EnvelopeCheck`, `A2aReader` and `Answers`, each built from a string with no directory: every refusal reason and limit of the envelope, and what an answer may carry."
}
```

**Why this size.** Tests only, of three classes in `filter/app`, no production change expected.

**What must be true.** Every rule of the envelope check and of the answer fails a unit test of its own class when it
is broken, without a mailbox directory.

## Why

No test of the `a2a` package exists. `EnvelopeCheck`, `A2aReader`, `MessageFacts` and `Unprocessable` - roles, ids,
peer names, unknown fields and their depth and size, a part with text and raw, broken JSON - are reached by
`MessageSluiceTest`, which builds an `EnvelopeCheck` directly twice and otherwise delivers files and reads where they
land. `Answers` - that a refusal never carries what it refused, keys named by position only, broken JSON answered
with name and hash only, every answer reading back through the check - is proven only by reading files from
`feedback/`.

## Plan

- Unit tests of `EnvelopeCheck` and `A2aReader` for each refusal reason and each limit, just inside and just
  outside, each envelope built from a string.
- Unit tests of `Answers` for what an answer may carry: no matched text, keys by position, broken JSON by name and
  hash, and the answer itself passing the envelope check.
- Red first: an answer test against a redaction broken on purpose.

## Acceptance

- Each refusal reason, limit and answer rule named under *Why* has a unit test that fails when the rule is broken on
  purpose, seen red.
- None of these tests touches a directory; the unit suite stays under a minute.

## Decided by the operator (2026-11-12)

- The test pyramid audit is built as three issues: [SL53](README.md#sl53) for the per-message decision, this one, and
  [SL55](README.md#sl55) for a watch's end.
