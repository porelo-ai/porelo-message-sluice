# SL46 — The Statistical Detectors Are Held To Their Rules

```json
{
  "$schema": "issue-1.json",
  "id": "SL46",
  "title": "The Statistical Detectors Are Held To Their Rules",
  "state": "Backlog",
  "size": "M",
  "priority": "Medium",
  "rank": 8,
  "blockedBy": [],
  "covers": "Each rule of the token-shape, entropy and letter-distribution detectors held just inside and just outside its limit (PIT)."
}
```

**Why this size.** three detectors whose rules no test tells apart from a rule taken out.

**What must be true.** Each rule of the token-shape, entropy and English-distribution detectors is held by a test that a text just inside the rule is found and one just outside passes.

## Plan

PIT left these mutants alive:

- `TokenShapeDetector.inspect`: the conditions of the vowel-share, digit-and-capital and symbol rules survive negated,
  and its counts (`countDigits`, symbols, punctuation, vowels) survive changed; `Tokens.isVowel` returning `true` and
  `Tokens.of`'s trimming survive.
- `EntropyDetector.inspect`: joining overlapping windows into a run, and reporting a run before the next one starts,
  survive; so does a text shorter than the minimum returning nothing.
- `DistributionDetector.chiSquarePerLetter`: returning 0 survives, so no test holds the letter-frequency rule; the
  whitespace share survives too.
- Red first: per rule a token or text just past its limit, and two high-entropy runs apart in one text, each found.

## Acceptance

- Each case named under *Plan* has a test that fails against the mutant PIT left, seen red by PIT or by breaking the
  check by hand, and passes against the code.
- PIT on the classes named, with the `pit` profile, leaves none of the places in *Plan* surviving, or this issue names
  the place and why no input tells it apart.
