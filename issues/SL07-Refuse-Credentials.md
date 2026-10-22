# SL07 — Refuse credentials

**Status:** Implementing.
**Size:** M - one detector of ten categories, each with its pattern and, where one exists, its validation or
context, and a false-positive measurement on real text. The personal and financial data are
[SL32](README.md#sl32).

**What must be true.** A message carrying a key, an API token, a private key, a connection string or a
password is refused, while ordinary English that merely talks about one is not.

## Why

This is the second of the three things the filter exists to catch. A credential looks statistically
like ordinary text, so the encoded-data checks ([`doc/detectors.md`](../doc/detectors.md)) do not
find it; a targeted catalog of **pattern, surrounding context and validation** does, and the validation
is what separates a finding from noise. The catalog is
[SL07-Refuse-Credentials_design.md](SL07-Refuse-Credentials_design.md).

## The shape

- Every text part, and every string in `metadata`, is checked against the catalog through the
  detector interface and nothing else. **It sees the text as normalized, not masked**, so a token
  inside a URL's query string cannot slip through the masking the encoded-data checks use.
- Every category can be switched off and re-graded between `INFO`, `SUSPICIOUS` and `BLOCKING`.
- Known test values - `AKIAIOSFODNN7EXAMPLE` - are in an extensible list and count as `INFO` only.
- A context word can carry a finding: *"my password is hunter2"*.

## Plan

- **`detect/CredentialDetector`** (`builtin/credentials@1`), a `ContentDetector` with the design's
  credential categories, each one rule `CREDENTIAL/<category>`: `PRIVATE_KEY` (header and body),
  `CLOUD_KEY`, `VCS_TOKEN`, `JWT` (its header decoded), `CONNECTION_STRING` (a URI parsed),
  `PASSWORD`, `SSH_AUTH` (`sshpass -p`), `API_KEY_GENERIC` (with a context word within 40 characters),
  all `BLOCKING`; `CERTIFICATE` and `HASH` `SUSPICIOUS`. Placeholders (`${…}`, `<…>`, `****`, `TOKEN`)
  are no value.
- **Switches and grades come from what exists:** a category's rule is switched off by
  `decision.disabledRules`, and the category re-graded by `decision.severity.<category>`. New are only
  `detector.credentials.enabled` and `detector.credentials.dummyValues`, wired in `Settings.detectors()`.
- **Offsets, not excerpts:** a rule whose value is a part of its match reports that part's offsets, and
  `DetectorRunner.toFinding` redacts them, so the matched value never reaches an answer.
- **Tests:** `CredentialsTest`, a sample per category in a sentence, built from pieces so that no
  scanner reading the repository takes the source for a leak; what only talks about a credential;
  fixtures named for where they land (`FixturesTest`); the corpus run of `EncodedPayloadRatesTest`
  now includes the detector; `doc/detectors.md` holds the catalog.
- **Red first:** with the default configuration an AWS key, a password in a sentence, an address with a
  password and `sshpass -p` all pass; each must be refused, and its category gone with its rule
  switched off.
- **Measured** on real text, not only the corpus: 27193 paragraphs agents wrote to each other. The
  first cut refused nine. Each cause was taken out with a case in `CredentialsTest`: a password word
  followed by a word in emphasis or before a full stop, a placeholder (`user:TOKEN@`), a private key's
  header named without its body, code after `passphrase =`, a file name with *password* in it, `sshpass
  -p` named in backticks. One stays,
  a command line quoted after *passphrase:*, and is written down. The 285 corpus paragraphs: none refused.
  The cost on the JVM, 5000 messages of 2 KB, three runs on a host under load (load 7 to 20): 6 to 26 %
  more time; not measured alone.
- **Read against the skills** (graal 2026.910.3; java-code-review, test-quality, security-audit,
  concurrency-review, clean-code and solid-principles 2026.906.3). security-audit's findings, each with a
  test in `CredentialsTest` seen red first:
  - two rules took over ten seconds on 64K characters shaped for it (an address scheme restarted at every
    word boundary, a JWT segment rescanned from every `eyJ`), and a key header of many words overflowed the
    stack. The rules are now anchored by a look-behind, bounded and possessive, and five such shapes are
    read within two seconds;
  - an address in backticks, quotes or angle brackets, or with a capitalized scheme, a password in JSON
    (`"password": "…"`) or PHP (`'password' => '…'`), a key whose line breaks are written as `\n`, and a
    key file whose `private_key` comes before its `type` passed. Each is found now.
  - java-code-review's and clean-code's points are taken: the patterns are compiled once, the sets built
    once, a context word ends at a word boundary. test-quality's assertion without its reason is fixed.
  - Not taken: the record of seven parts, which only reads worse. Not this issue's: in the default
    `PARTIAL` redaction the first four characters of a long finding appear in the answer, for every rule.
- **Not here:** the design's infrastructure categories (`INTERNAL_HOSTNAME`, `FILE_PATH`, `STACK_TRACE`,
  `SQL_STATEMENT`) are no credential, and *What must be true* does not ask for them.

## Acceptance

- **One sample per category** inside a natural English sentence (*"Sure, the key is AKIA…"*) is
  refused in blocking mode (`HASH` and `CERTIFICATE` are suspicious), and a credential in `metadata`
  rather than a text part is found too.
- A JWT header alone, a password only talked about, an address without a password and a key's header
  without its body **must not** cause a credential finding.
- The rejection for every sample **must not** contain the matched value in clear text.
- False positives stay under 1 % against the clean English the encoded-data checks are measured on.
- Each sample's category is gone with its rule switched off, and each sample was seen to pass before
  the detector existed.
