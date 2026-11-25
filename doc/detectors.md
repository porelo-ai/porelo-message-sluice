# What the filter looks for in the text

After the envelope ([`filter.md`](filter.md)), every text and every string in a message goes through
the **detectors**. The ones built catch **encoded data**, **credentials** and **personal and financial
data**. A payload spread over several messages is not built yet.

A detector only reports findings; the verdict is decided on all of them together
([`filter.md`](filter.md#the-verdict)), and every excerpt is masked before it goes anywhere.

## Before the detectors

Every text is normalized once, and every finding still points into the original text:

- **Unicode NFKC**, and line endings made one.
- **Invisible characters are removed and reported** (`UNICODE/INVISIBLE_CHARACTER`): zero-width
  characters, bidi controls, tag characters, the soft hyphen and variation selectors. A few are
  `SUSPICIOUS`, eight or more in one text `BLOCKING`. The joiners and selectors that make up an
  ordinary emoji are not reported.
- **Look-alike letters are read as Latin** in a word that mixes Latin with Cyrillic or Greek, and the
  word is reported (`UNICODE/MIXED_SCRIPT`, `SUSPICIOUS`): *"раssword"* with a Cyrillic `р` and `а` is
  read as *"password"*.

For the statistical checks, shapes that are known to be harmless are taken out first: URLs (but not
their query, and not an over-long path or host label), up to four UUIDs, up to eight short commit
hashes, IP addresses, timestamps, numbers, dates, amounts and short acronyms. More UUIDs or hashes than
that are treated as the payload they then are.

## The rules

| Detector | Rule | Severity | What it catches |
|---|---|---|---|
| `builtin/long-token@1` | `ENCODING/LONG_TOKEN` | `BLOCKING` | more than 35 characters without a space or a separator |
| `builtin/token-shape@1` | `ENCODING/DIGIT_UPPER_TOKEN` | `SUSPICIOUS` | a word of more than 8 characters, over half digits and capitals |
| | `ENCODING/VOWELLESS_TOKEN` | `SUSPICIOUS` | a word of more than 12 characters with under 15 % vowels |
| | `ENCODING/SYMBOL_HEAVY_TOKEN` | `SUSPICIOUS` | over 20 % of `+ / = % _ -` in a mostly alphanumeric word |
| | `ENCODING/PUNCTUATION_HEAVY_TOKEN` | `SUSPICIOUS` | over 20 % of other punctuation |
| `builtin/entropy@1` | `ENCODING/WINDOW_ENTROPY` | `BLOCKING` | a 64-character stretch above 4.5 bits per character |
| `builtin/english-distribution@1` | `ENCODING/LETTER_DISTRIBUTION` | `SUSPICIOUS` | 200 characters or more whose letters are not distributed like English |
| | `ENCODING/WHITESPACE_SHARE` | `SUSPICIOUS` | the same, with under 8 % whitespace |
| `builtin/encoding-signature@1` | `ENCODING/BASE64`, `BASE64URL`, `BASE58`, `BASE32`, `BASE85`, `HEX`, `URL_ENCODING`, `QUOTED_PRINTABLE`, `UUENCODE`, `BINARY_STRING`, `PEM`, `DATA_URI`, `DECIMAL_BYTES` | `BLOCKING` | the named encoding: a base-N, hex or uuencoded run from 32 characters on (`detector.signature.minLength`); a PEM header, a data URI's `;base64,` prefix, ascii85 between `<~ ~>`, a uuencode `begin` line, 16 hex bytes with separators or 32 bits of binary on their own; in code, 16 numbers a byte holds (-128 to 255, perhaps cast or written as a long) in a list separated by commas; six URL or quoted-printable escapes, each within 16 characters of the last |

**Two different `SUSPICIOUS` rules in one message refuse it**; one rule firing many times counts once.

**Base64 is told from an identifier by how it switches case**: words keep a run of lower-case letters
after a capital, random letters switch every one or two characters. Words joined by dashes or
underscores are a name, not base64url.

**A full hash in hex is refused**, because 32 hex characters of a payload and an MD5 look the same.
Name a commit by its short form.

**A payload cut into pieces is the payload.** Pieces of one alphabet that stand in a row with only
whitespace between them - spaces, line breaks, tabs, or `\n` written out - are joined and judged as
one run, under the encoding's own rule. Words, numbers and names joined by dashes or slashes at
either end of such a row are the sentence around it and are left out; a row that is more than two
thirds words is prose and is never joined. Measured on 13342 paragraphs agents wrote to each other:
no more than three short commit hashes ever stood in a row, and two paragraphs of the 13342 - a
table and a command line - are refused by this alone.

## Credentials

A credential can look like ordinary text, so the encoded-data checks miss it. `builtin/credentials@1`
looks for what it is instead: a pattern, a validation where one exists, and where the value alone says
nothing a context word near it. It reads the normalized text, not the masked one, so a token in a URL's
query string is seen too. Every category is one rule, `CREDENTIAL/<category>`: `decision.disabledRules`
switches it off and `decision.severity.<category>` re-grades it.

| Category | Severity | What it catches |
|---|---|---|
| `PRIVATE_KEY` | `BLOCKING` | a private key's header followed by its body; a header alone is talk about the format |
| `CLOUD_KEY` | `BLOCKING` | an AWS access key id, an Azure connection string's key, a Google service account's key file |
| `VCS_TOKEN` | `BLOCKING` | a GitHub, GitLab, npm or PyPI token by its prefix and length |
| `JWT` | `BLOCKING` | three base64url segments whose first decodes to JSON with `alg` |
| `CONNECTION_STRING` | `BLOCKING` | an address with a user name and a password that is no placeholder |
| `PASSWORD` | `BLOCKING` | *password*, *passwd* or *passphrase*, then *is*, `:` or `=`, then a value with a letter and a digit or a symbol |
| `SSH_AUTH` | `BLOCKING` | a password handed to `sshpass -p` |
| `API_KEY_GENERIC` | `BLOCKING` | twenty characters or more with a stretch of sixteen mixing both cases and a digit, within 40 characters of *api key*, *secret*, *token*, *bearer* or *credential* |
| `CERTIFICATE` | `SUSPICIOUS` | a certificate's header |
| `HASH` | `SUSPICIOUS` | a bcrypt, argon2 or crypt password hash |

**A placeholder is not a value:** `${DB_PASSWORD}`, `<password>`, `****`, `TOKEN`. **A known test
value** (`detector.credentials.dummyValues`) is reported as `INFO` only: the providers' documented
example keys and *hunter2*.

## Personal and financial data

`builtin/personal-data@1` finds them by pattern and, where one exists, by checksum: the checksum tells a
card number or an IBAN from any other run of digits. What may belong to a business - an address to
write to, a number to call - is `SUSPICIOUS`, and a project with a strict data-protection reading sets
`decision.severity.EMAIL` (or `PHONE`) to `BLOCKING`. Every category is one rule, `PERSONAL/<category>`.

| Category | Severity | What it catches |
|---|---|---|
| `CREDIT_CARD` | `BLOCKING` | 13 to 19 digits, spaces or dashes allowed, that pass the Luhn checksum and carry a card network's prefix and length |
| `IBAN` | `BLOCKING` | a country code, two check digits and the account, grouped or not, that pass MOD-97 |
| `NATIONAL_ID` | `BLOCKING` | a Swiss social insurance number (`756.…`) that passes its EAN-13 check digit; a US social security number named as one |
| `EMAIL` | `SUSPICIOUS` | an address with a domain whose top level is letters, none of the documentation domains (`example.com`, `.test`, `.invalid`) and no `git@host:` remote |
| `PHONE` | `SUSPICIOUS` | an international number (`+41 …`), or a number of 7 to 15 digits after *phone*, *tel*, *mobile* or *call me at* |
| `IP_ADDRESS` | `SUSPICIOUS` | a public IPv4 address: not private, loopback, link-local, shared, multicast or a documentation range, and no version after *Temurin*, *GraalVM*, *snapshot* or *v* |
| `DATE_OF_BIRTH` | `SUSPICIOUS` | a date after *born*, *date of birth* or *DOB* |
| `POSTAL_ADDRESS` | `INFO` | a number and a street (*221 Baker Street*, *Bahnhofstrasse 12*) |

**Known test values** (`detector.personalData.dummyValues`) are `INFO` only: the card and account
numbers payment documentation uses as examples, and the public resolvers `1.1.1.1`, `8.8.8.8` and
`9.9.9.9`.

## Code

A review comment may suggest a few lines of code to replace the lines it is anchored to. Code is not
prose, so **a suggested replacement, a file's path, and a URL's host, each of its path segments and its
whole path in a review document are judged as code**, segments on their own and together, so no URL
masking hides one: by the
long-token check, the encoding signatures and the entropy window, each with thresholds of its own
measured on code, and by the credential and personal data detectors as they are. The letter shape and
distribution tests, which hold English to English, do not see code. A detector's `enabled` switches
its code twin too.

## Configuration

Every threshold is a key, `detector.<name>.<parameter>`, and every detector can be switched off with
`detector.<name>.enabled=false`. An unknown key stops the start.

| Key | Default | Where it comes from |
|---|---|---|
| `detector.longToken.maxTokenLength` | 35 | the longest common English word is about 30 characters |
| `detector.tokenShape.maxDigitUpperShare` | 0.5 | the design |
| `detector.tokenShape.minVowelShare` | 0.15 | the design |
| `detector.tokenShape.maxSymbolShare` | 0.2 | the design |
| `detector.entropy.threshold` | 4.5 | measured, below |
| `detector.entropy.windowSize` / `step` / `minLength` | 64 / 16 / 64 | measured: 64-character windows tell base64 from English, 128 do not |
| `detector.distribution.minLength` | 200 | shorter is noise |
| `detector.distribution.maxChiSquarePerLetter` | 2.0 | measured, below |
| `detector.distribution.minWhitespaceShare` | 0.08 | measured, below |
| `detector.signature.minLength` | 32 | the shortest payload that must not pass |
| `detector.longToken.code.maxTokenLength` | 80 | measured on code: test method names reach about 71 |
| `detector.entropy.code.threshold` | 4.3 | measured on code, below |
| `detector.entropy.code.windowSize` / `step` / `minLength` | 64 / 16 / 64 | as for prose; 32 and 48 measured no better |
| `detector.signature.code.minLength` | 32 | the shortest payload that must not pass |
| `detector.credentials.dummyValues` | AWS's two documented example keys, `hunter2` | values known to be no secret, compared exactly |
| `detector.personalData.dummyValues` | the documented example card and IBANs, the public resolvers | values known to be nobody's, compared without spaces, dots and dashes |

## Calibration

```
porelo-message-sluice-filter --calibrate --dir <task state>/mail/sent > calibrated.properties
```

It reads a directory of messages, moves nothing, and prints a properties file for `--config`: how many
messages blocking would refuse and by which rule, and for each statistical measure a suggested value
between that traffic and random base64. Where the two overlap, it keeps the current value and says so.
**The default thresholds are fit for blocking from the start**; calibration shows how a mailbox's own traffic fares, when it is
refused more often than it should be.

## How well it works

With the defaults:

- **English that must pass:** every paragraph of this repository's documentation, none refused (a test
  keeps it so), and 2869 paragraphs agents wrote to each other, with paths, hashes and command lines:
  one refused.
- **Encodings that must not pass:** 500 random samples of each encoding at 32, 48 and 64 characters,
  each inside a sentence: under 1 % missed for every one (a test keeps it so). The same payloads cut into
  groups of 2 to 24 characters by spaces, line breaks, tabs or written-out escapes: under 1 % missed for
  base64, base32, base58 and hex (a test keeps it so).
- **Credentials:** a sample of every category inside a sentence is found, and its value is in no
  finding (a test keeps it so). In 27193 paragraphs agents wrote to each other, one is refused for a
  credential: a command line quoted after *passphrase:*. Every paragraph of the documentation passes.
- **Personal data:** a sample of every category is found with its severity, and an invalid card number
  or IBAN is none (a test keeps it so). In 27250 paragraphs agents wrote to each other, 14 more are
  refused than without it, each for a suspicious address beside another suspicious rule: an e-mail
  address in a package's maintainer field, or the public address of a rented machine.
- **Code that must pass:** 1245 snippets of 1 to 12 lines frozen from this repository's own Java
  sources, the size of a suggested replacement, with the test payloads left out: 8 refused (0.64 %; a
  test keeps it at 10 at most): a PEM header, a SHA-256 test value, three test lines that read as
  base85, a long test method name with digits, a random test key and a Cyrillic string literal. Judged by the
  prose detectors instead, 83 are refused.
- **Code the thresholds were not set on:** snippets cut the same way from other Porelo repositories,
  with the test files that hold keys, tokens, hashes or commit ids left out (a test holds each language's
  count; Java and Dart stay under 1 %, YAML and shell do not):

  | Language | Snippets | Refused | Refused for |
  |---|---|---|---|
  | Java | 791 | 2 (0.25 %) | a PEM header named in a comment, method calls that read as base85 |
  | Dart | 445 | 1 (0.22 %) | a made-up commit id in mock data |
  | YAML | 299 | 10 (3.3 %) | actions pinned by commit id, image and download digests |
  | Shell | 277 | 3 (1.1 %) | download digests, a Python program in a string, a long run of messages |

- **A lockfile's checksums:** every `sha256` line of a `pubspec.lock`, 109, is refused as hex.
- **Encodings in code that must not pass:** 500 random samples of each encoding at 32, 48 and 64
  characters, each as a string literal in a line of Java: at 32 characters 1 base58 and 2 base85
  missed, none else (a test keeps every one under 1 %). Uuencode needs letter bounds of its own in code
  (a letter share of 0.15 and a mean letter run of 3.6, against 0.2 and 3.2 in prose), since a
  literal's quotes and `;` thin out its letters.
- **Encodings hidden in code:** the same payloads, 500 samples of each with a seed of their own, in a
  snippet the detectors accept alone, written as an agent would hide them (a test holds each count):

  | Form | 32 characters | 48 characters | 64 characters |
  |---|---|---|---|
  | 2 to 4 literals of at most 31 characters joined by `+` on one line | 1.8 % base85, 0.4 % uuencode, 0.2 % base64url and quoted-printable missed, none else | 1 % base85, none else | none |
  | the same literals, one per line | 2.6 % uuencode, 2 % base85, 0.2 % base64url and base58, none else | 0.8 % uuencode, 0.4 % base85, none else | none |
  | a `byte` array of the characters in hex (`0x41, 0x42`) | none | none | none |
  | an `int` array of the characters in decimal (`65, 66`) | none | none | none |
  | `\uXXXX` escapes in one literal | none | none | none |
  | `\uXXXX` escapes in literals of at most 12 characters each, one per line | none | none | none |

  Literals joined by `+` or side by side are judged as the one string they make, besides the code as
  written. A byte array is caught as hex bytes with separators, an int array as decimal bytes;
  escapes `\uXXXX` (also `\uuuuXXXX`), `\u{X}`, `\UXXXXXXXX` and `\xXX` are read as the characters
  they write, an invisible one as nothing, and held as one long
  token as well once their literals are joined.
- **The thresholds:** English windows reach at most 4.56 bits per character, base64 has a median of
  4.75; English letters reach at most 1.64 chi-squared per letter, base64 at least 2.69; English has at
  least 8.7 % whitespace, base64 none.

## What these detectors cannot see

- **A credential with no shape and no word naming it**: a password written alone, a key of a provider
  the catalog does not know, a token whose prefix appeared after this catalog.
- **A secret described rather than written** (*"the dog's name plus 77"*).
- **Personal data in prose**: a name, a street written out without its number, a national id of a
  country other than Switzerland and the US, an IPv6 address, a phone number with neither `+` nor a
  word naming it.

- **Fewer than 32 characters of an encoding** that nothing else gives away.
- **A payload in code cut into string literals that are not joined in the code**: passed as separate
  arguments, joined through a variable, `+=`, `concat`, `plus`, `String.join` or another call, put
  together at run time, or literals with a prefix (`f"…"`, `r"…"`, `R"(…)"`) or with a `\` before the
  closing quote. Literals joined by `+` or side by side, with whitespace, comments or one pair of
  parentheses between them, are read as one.
  Of those read as one, at 32 characters up to 2.6 % of uuencode and base85 still pass.
- **Characters written in code by other escapes**: octal (`\101`), HTML or XML entities, or a call
  such as `fromCharCode` or `chr`; only `\uXXXX`, `\u{X}`, `\UXXXXXXXX` and `\xXX` are read.
- **Fewer than 16 bytes in a decimal array**, or one whose runs a value no byte holds, a hex or binary
  value, a character literal or a comment cuts below 16; or numbers separated by anything but commas.
- **A payload in a URL path** of short segments, up to 256 characters; the query is checked.
- **Up to four UUIDs and eight short hashes per text**, which pass as ids: about 64 bytes per message.
- **A review document's URLs, within their shape**: the port and the pull request's number (up to 100
  digits) are held to their grammar alone; a host and path of fewer than 64 characters, the entropy
  window's length, passes when no run of an encoding in it reaches 32 characters (cut by `.` or `~`,
  which no encoding uses), as do the shapes the detectors take for ids, a UUID or a short hash, inside a
  segment.
- **The two full commit ids of a review document** (`revisionId`, `porelo/baseRevisionId`), held to
  their shape alone: up to 64 bytes a task chooses. Whether they name real commits is the host's to
  check.
- **Anything that is not English prose**, which the letter test may refuse.
- **Code quoted at length** may be refused: long identifiers, shell commands, fingerprints, a table of
  16 or more small numbers, and in
  workflows, playbooks and scripts the commit ids and digests that pin what they download.
  Calibration shows how often that happens in a mailbox's own traffic.
