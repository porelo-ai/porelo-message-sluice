# SL44 — Every Encoding Signature Is Held

**Status:** Backlog.
**Size:** M - many small cases in one detector, each with a payload and with the prose next to it.

**What must be true.** Each encoding `EncodingSignatureDetector` names is refused by a test of its own, and each of its exemptions for prose is held on both sides: uuencode, ascii85, hex bytes, dense escapes, and runs whose alphabet says base32, base58, base64 or base64url.

## Plan

PIT left these mutants alive in `EncodingSignatureDetector`:

- `inspect`: removing the search for hex bytes, uuencode or ascii85 survives; uuencode and ascii85 are not covered at
  all.
- `dense`: the first dense run of escapes and the count limits survive; one is not covered.
- `isProse`, `judge`, `hasBothCases`, `isHex`, `isJoinedWords`: returning `true` survives, so a payload could be taken
  for prose, and the share of prose in a run is held by no test.
- `alphabetRun`: the counts of vowels, `+`/`/`, `-`/`_` and the base58 alphabet, and the randomness test, survive.
- `letterShare`, `spaceShare`, `punctuationShare`, `distinctPunctuation`: their filters survive.
- Red first: one payload per encoding next to the prose closest to it that must pass, measured on agents' prose as the
  thresholds were.

## Acceptance

- Each case named under *Plan* has a test that fails against the mutant PIT left, seen red by PIT or by breaking the
  check by hand, and passes against the code.
- PIT on the classes named, with the `pit` profile, leaves none of the places in *Plan* surviving, or this issue names
  the place and why no input tells it apart.
