# SL45 — Invisible Characters And Look-Alikes Are Held

**Status:** Analyzed.
**Size:** S - tests of `Normalizer` only.

**What must be true.** Every kind of invisible character the normalizer looks for is found by a test, a joiner counts as part of an emoji only after a symbol, and a word mixing Latin with look-alikes is found.

## Plan

PIT left these mutants alive in `Normalizer`:

- `isInvisible`: two of its kinds survive, two are not covered.
- `isEmojiJoiner`: returning `true` survives, so a joiner between letters would pass as part of an emoji.
- `foldMixedScripts`: the test for a Latin letter survives.
- `normalize`: the sorting of invisible runs and the line-ending step survive.
- Red first: one text per kind of invisible character, a joiner between two letters, and a word of Latin and Cyrillic
  letters, each found.

## Acceptance

- Each case named under *Plan* has a test that fails against the mutant PIT left, seen red by PIT or by breaking the
  check by hand, and passes against the code.
- PIT on the classes named, with the `pit` profile, leaves none of the places in *Plan* surviving, or this issue names
  the place and why no input tells it apart.
