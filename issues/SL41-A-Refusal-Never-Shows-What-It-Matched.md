# SL41 — A Refusal Never Shows What It Matched

**Status:** Backlog.
**Size:** S - the tests and at most a line of `DetectorRunner` and `EnvelopeResult`; the most direct harm of the set.

**What must be true.** A refusal never carries what a rule matched: a finding's explanation that quotes the matched text is refused, and every key of a message is named by its position in an answer whenever the detectors did not vouch for it.

## Plan

PIT with the `pit` profile left these mutants alive; no test fails when the check is taken out:

- `DetectorRunner.quotes` and `isPlainWord`: the check that refuses a finding whose explanation quotes what it matched.
  Returning `false` from `quotes`, or `true` from `isPlainWord` for a mixed-case word, survives: an external detector's
  explanation could carry the secret into an answer.
- `EnvelopeResult.withKeysHidden`: when the detectors did not run, every key is hidden. Negating `!detectorsRan`
  survives, so keys would be shown in clear; `under`'s exact match on the key itself survives, and so does its match of the key's own value, `key(`, when no
  child of the key is in the message: a key a rule matched with a plain value next to it would be shown.
- Red first: a detector stand-in whose explanation repeats the match, a quote spread inside a longer word in mixed case,
  and a message whose key carries a secret refused before the detectors ran; each answer is read for the secret.

## Acceptance

- Each case named under *Plan* has a test that fails against the mutant PIT left, seen red by PIT or by breaking the
  check by hand, and passes against the code.
- PIT on the classes named, with the `pit` profile, leaves none of the places in *Plan* surviving, or this issue names
  the place and why no input tells it apart.
