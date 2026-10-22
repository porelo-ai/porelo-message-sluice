# SL32 — Refuse personal and financial data

**Status:** Backlog.
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

## Acceptance

- One sample per category inside a natural English sentence is found with its default severity, and an
  e-mail address or a phone number is refused once `decision.severity.EMAIL` (or `PHONE`) says
  `BLOCKING`.
- Invalid credit card numbers and IBANs **must not** cause a refusal.
- The rejection for every sample **must not** contain the matched value in clear text.
- False positives stay under 1 % against the clean English the encoded-data checks are measured on.
