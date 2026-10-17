# SL24 — A code comment, the filter's side

**Status:** Estimated; blocked by Porelo [B166](https://github.com/porelo-ai/porelo/blob/main/issues/base/README.md#b166), which defines the comment model and what the host checks, and by
[SL23](README.md#sl23), whose narrowed schema it adds a kind to.
**Size:** L - new envelope rules and a second set of detector thresholds measured on a code corpus,
for the shape [B166](https://github.com/porelo-ai/porelo/blob/main/issues/base/README.md#b166) sets.

**What must be true.** A comment on code - a message of the kind `comment` - passes the filter only in
its exact shape: an anchor that names a place, prose that is prose, and at most a suggestion that is
a few lines of code for exactly that place. Anything else in it is refused by a rule that names what
is wrong, in both directions: what a task sends to a review or a forge, and what arrives from a forge.

## Why

A comment is a message, so it passes the controls a message passes. The filter refuses data dressed
as prose ([`doc/filter.md`](../doc/filter.md#the-envelope)); a comment brings two things it has no
rule for today:

- **The anchor names a commit by its id.** A 40-digit hex string is exactly what the detectors refuse
  in any field. Judged as text, every comment would be refused; let through unchecked, the anchor is
  a field an agent can fill with anything.
- **A suggestion is code, not prose.** The detectors are measured against English prose
  (`EncodedPayloadRatesTest`); code has other letter, symbol and token shapes, and would be refused
  for being code. Let through unjudged, a suggestion would be a field for a payload of any size.

## The shape

- **The anchor is checked by its shape, field by field, and is not text:** repository a peer name,
  commit 40 or 64 lower-case hex digits, path a relative path (no `..`, no leading `/`, bounded
  length, printable characters only), side `old` or `new`, and first and last line, at most 100 lines
  apart, as [B166](https://github.com/porelo-ai/porelo/blob/main/issues/base/README.md#b166) sets it. Whether that commit, path and lines exist is the host's to check; the
  filter only makes sure the anchor can hold nothing but a place.
- **The text is prose**, judged by the detectors as every text part is, with today's length limit.
- **The suggestion is a field of its own, optional:** at most as many lines as the anchor spans plus
  10, at most 400 characters a line and 8 KiB in all, as [B166](https://github.com/porelo-ai/porelo/blob/main/issues/base/README.md#b166) sets it. It goes to the detectors that find encoded
  data (long tokens, signatures, window entropy), with thresholds measured against code rather than
  prose; the detectors that judge whether text reads like a language do not see it. A suggestion
  without an anchor, or longer than its bounds, is refused by its own rule.
- **Thread fields** (thread id, reply-to) follow today's id rule; the state is `open` or `resolved`.
- **The origin is not the sender's to state.** A comment that names its own origin is refused; the
  host adds who it came from after the filter.
- **The same rules both ways.** An inbound comment from a forge is held to the same shape and the same
  detectors before it is delivered into a task; which forge accounts are delivered at all is the
  host's list, not the filter's.
- **`comment` is a sixth kind of [SL23](README.md#sl23)'s narrowed schema**, as [B166](https://github.com/porelo-ai/porelo/blob/main/issues/base/README.md#b166) decides, so it
  is built on [SL23](README.md#sl23)'s envelope rules.
- **One rule per refusal**, named in the answer, as for every envelope rule.

## Plan

- **`a2a/EnvelopeCheck`:** the anchor's fields checked by shape and not added to the texts the
  detectors read; the text added as a text part is; an origin field refused; the suggestion's bounds
  checked against the anchor's line span. Each refusal a rule of its own.
- **Code judged as code:** `detect/TextUnderCheck` gains what it holds (prose or code), and
  `detect/DetectorRunner` gives a code text only to the encoded-data detectors (`LongTokenDetector`,
  `EncodingSignatureDetector`, `EntropyDetector`), each with thresholds of its own for code
  (`detector.<name>.code.*` in `Settings`). `TokenShapeDetector` and `DistributionDetector`, which
  judge whether text reads like a language, do not see it.
- **The code corpus:** real lines from this repository's sources, frozen as a test resource beside
  `english-corpus.txt`, and a rates test like `EncodedPayloadRatesTest` measuring its false positives
  and the misses on 32 characters of payload.
- **Red first:** a comment whose anchor carries a 40-digit commit id is refused today as a long hex
  token; it must pass, and the same comment with a base64 blob in its suggestion must be refused.

## Acceptance

- One fixture per refused shape - a malformed commit id, a path with `..`, a line range beyond its
  bound, a side other than the two, an origin field, a suggestion without an anchor, a suggestion too
  long, a base64 blob inside a suggestion - each refused by its own rule, each test seen to fail with
  that rule removed.
- A comment in the exact shape passes, with and without a suggestion, inbound and outbound.
- **A code corpus** beside the English one: real code lines from this repository's own sources, frozen
  as a test resource, on which the suggestion's detectors measure their false positives, and 32
  characters of encoded payload per encoding on which they measure their misses, as for prose.
