# SL32 — Refuse personal and financial data

**Status:** Implementing.
**Size:** M - a second rule set beside [SL07](README.md#sl07)'s credentials, with checksums for cards and
IBANs and a false-positive measurement on real text.

**What must be true.** A message carrying a payment card number, an IBAN or a national id is refused, an
e-mail address, a phone number, an IP address or a date of birth is reported as suspicious, while ordinary
English with numbers in it is not.

## Plan

- **The design's personal and financial categories**, in the design of [SL07](README.md#sl07): `CREDIT_CARD` (Luhn and the
  issuer's prefix), `IBAN` (MOD-97) and `NATIONAL_ID`, `BLOCKING`; `EMAIL`, `PHONE`, `IP_ADDRESS` and
  `DATE_OF_BIRTH`, `SUSPICIOUS`; `POSTAL_ADDRESS`, `INFO`. Each one rule, `PERSONAL/<category>`, switched
  off and re-graded as [SL07](README.md#sl07)'s are. `4111 1111 1111 1111` joins the known test values.
- **The checksum is what separates a finding from noise:** an invalid card number or IBAN is no finding.
- **Measured** as [SL07](README.md#sl07)'s: the documentation corpus and the paragraphs agents wrote to each other.

- **Built** as `detect/PersonalDataDetector` (`builtin/personal-data@1`), the catalog in `doc/detectors.md`.
  `NATIONAL_ID` is the Swiss social insurance number by its EAN-13 check digit and the US social security
  number named as one. Every pattern is bounded, and those that start inside a run are anchored by a look-behind; six shapes of 64K
  characters made to slow it are read within two seconds (`PersonalDataTest`).
- **Red first:** with the detector a stub, every sample passed and `decision.severity.EMAIL` was refused as
  an unknown category.
- **Measured** on 27250 paragraphs agents wrote to each other: the first cut refused 16 more than without
  it. A version (*Temurin 21.0.8.1*) and the public resolvers read as public addresses; each is taken out
  with a case in `PersonalDataTest`. 14 stay, each a real e-mail address (a package's maintainer) or a
  rented machine's public address beside another suspicious rule. The documentation corpus: none.
- **Read against the skills** (graal 2026.910.3; java-code-review, test-quality, security-audit,
  concurrency-review, clean-code and solid-principles 2026.906.3). No pattern is slow; security-audit's
  misses are found now, each with a case seen red first: a card followed by its expiry date or written with
  dots, a lower-case IBAN and one followed by another group, a Swiss number without dots, an address with
  letters beyond ASCII (whose first part redaction had left visible) or followed by a colon, and a public
  address after a bare `v`. Measured again on the paragraphs agents wrote: the lower-case IBAN let four
  issue numbers before words pass MOD-97 by chance, so an IBAN takes one case throughout and an account
  of mostly digits; the four are cases in `PersonalDataTest`. Still 14 more refused than without the
  detector, as above.

## Acceptance

- One sample per category inside a natural English sentence is found with its default severity, and an
  e-mail address or a phone number is refused once `decision.severity.EMAIL` (or `PHONE`) says
  `BLOCKING`.
- Invalid credit card numbers and IBANs **must not** cause a refusal.
- The rejection for every sample **must not** contain the matched value in clear text.
- False positives stay under 1 % against the clean English the encoded-data checks are measured on.
