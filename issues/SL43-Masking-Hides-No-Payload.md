# SL43 — Masking Hides No Payload

**Status:** Backlog.
**Size:** S - tests of `Masked` only.

**What must be true.** What `Masked` takes out of the text for the English checks is only what its documentation names: a host no DNS name could be, a chain of UUIDs, more short hashes than its limit, a long URL path segment and a URL's user part stay for the checks.

## Plan

PIT left these mutants alive in `Masked`:

- `hostLabels`: the character test of a label, the port test, the label and host length limits and the `null` for an
  impossible host all survive; one return is not covered. A payload written as a host would be masked.
- `touchesUuid`: the test that a UUID touches another survives in seven forms: a chain of UUIDs, which is hex, would be
  masked.
- `of`: the limit of short hashes survives negated: many hashes would be masked as ids.
- `urlSpan`: the path-segment length, the length of the masked URL, and the user part before `@` survive.
- Red first: a URL with an impossible host, a port of letters, a chain of two UUIDs, nine short hashes, a path segment
  of 49 characters and a user part carrying hex, each found by the encoding checks.

## Acceptance

- Each case named under *Plan* has a test that fails against the mutant PIT left, seen red by PIT or by breaking the
  check by hand, and passes against the code.
- PIT on the classes named, with the `pit` profile, leaves none of the places in *Plan* surviving, or this issue names
  the place and why no input tells it apart.
