# SL42 — Every Part Of A Task Is Walked

**Status:** Backlog.
**Size:** S - tests of `EnvelopeCheck` only; the walk exists, no test holds it.

**What must be true.** Every field of a task or message envelope that can carry text reaches the detectors and the envelope's limits: a task's artifacts, their parts, metadata and extensions, a part's URL, a task's history, and anything shaped like A2A 0.3 at any depth.

## Plan

PIT left these mutants alive in `EnvelopeCheck` and its walk:

- `task`: removing the walk of `artifacts[]`, of `metadata`, the unknown-field check, or the index of `artifacts` and
  `history` survives; `status.timestamp` is not covered. A payload in a task's artifact would pass unread.
- `artifact`: removing `unknownFields`, `parts`, `metadata`, `extensions` survives; its free text is not covered.
- `part`: removing `unknownFields`, the walk of `url` and of `metadata` survives, and the `binary` branch is untested.
- `message`: removing `extensions` survives. `extensions` itself is not covered at all: no test sends one.
- `rejectVersion03`: its descent into children and arrays, and the shape tests on `role` and `status`, survive.
- `walkValue`: counting a key's characters toward the metadata limit, and depth plus one, survive: the limits on
  metadata could be passed around.
- Red first: one message per field above carrying a payload or an unknown field, each refused; a 0.3 shape nested in an
  array; metadata just over its character and depth limit made of keys rather than values.

## Acceptance

- Each case named under *Plan* has a test that fails against the mutant PIT left, seen red by PIT or by breaking the
  check by hand, and passes against the code.
- PIT on the classes named, with the `pit` profile, leaves none of the places in *Plan* surviving, or this issue names
  the place and why no input tells it apart.
