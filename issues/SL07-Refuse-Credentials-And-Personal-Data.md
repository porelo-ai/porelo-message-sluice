# SL07 — Refuse credentials and personal data

**Status:** Estimated.
**Size:** L - one module, but about twenty categories, each with its pattern, context and validation,
and a false-positive measurement; it splits into credentials first and personal data after, if wanted.

**What must be true.** A message carrying a key, an API token, a private key, a connection string,
or personal or financial data is refused, while ordinary English that merely looks like one is not.

## Why

This is the second of the three things the filter exists to catch. A credential looks statistically
like ordinary text, so the encoded-data checks ([`doc/detectors.md`](../doc/detectors.md)) do not
find it; a targeted catalog of **pattern, surrounding context and checksum** does, and the checksum
is what separates a finding from noise. How it would be built is
[SL07-Refuse-Credentials-And-Personal-Data_design.md](SL07-Refuse-Credentials-And-Personal-Data_design.md).

## The shape

- Every text part, and every string in `metadata`, is checked against the catalog through the
  detector interface and nothing else. **It always sees the original text**, so a token inside a
  URL's query string cannot slip through the masking the encoded-data checks use.
- Every category can be switched on or off and re-graded between `INFO`, `SUSPICIOUS` and
  `BLOCKING`, with the defaults in the design. `EMAIL` and `PHONE` are `SUSPICIOUS` by default; a
  project with a strict data-protection reading tightens them to `BLOCKING`.
- Known test values - `4111 1111 1111 1111`, `AKIAIOSFODNN7EXAMPLE` - are in an extensible list and
  count as `INFO` only.
- A context word alone can carry a finding: *"my password is hunter2"*.

## Plan

- **`detect/CredentialDetector`**, a `ContentDetector` with the design's catalog: each rule a pattern, a
  ±40-character context and, where one exists, a validation (`detect/Validators`: Luhn, MOD-97, a JWT
  header decoded, a URI parsed). Unlike the encoded-data detectors it does not go through `Masked`, so
  it sees the text as normalized, query strings included.
- **Switches and grades come from what exists:** a category's rule is switched off by
  `decision.disabledRules`, and the category re-graded by `decision.severity.<category>` (`Settings`,
  `DecisionConfig`), so each category is one rule id. New are only
  `detector.credentials.enabled` and `detector.credentials.dummyValues`, wired in `Settings.detectors()`.
- **Offsets, not excerpts:** findings go through `DetectorRunner.toFinding`, which redacts them, so the
  matched value never reaches an answer.
- **Tests:** a sample per category in a sentence, as fixtures named for where they land
  (`FixturesTest`); the invalid numbers and JWT headers as passing fixtures; the false-positive rate
  added to `EncodedPayloadRatesTest`'s corpus run; `doc/detectors.md` gains the catalog.
- **Red first:** a sentence with an `AKIA…` key passes today; it must be refused, and pass again with
  `CLOUD_KEY` disabled.

## Acceptance

- **One sample per category** inside a natural English sentence (*"Sure, the key is AKIA…"*) is
  refused in blocking mode, and a credential in `metadata` rather than a text part is refused too.
- Invalid credit card numbers, IBANs and JWT headers **must not** cause a refusal.
- The rejection for every sample **must not** contain the matched value in clear text.
- False positives stay under 1 % against the clean English the encoded-data checks are measured on.
- Each sample's test is seen to fail with its category switched off.
- An e-mail address or a phone number is a `SUSPICIOUS` finding by default, and refused once
  `decision.severity.EMAIL` (or `PHONE`) says `BLOCKING`.

## To be checked

- One issue or two: credentials first and personal data after - decided when it is started.
