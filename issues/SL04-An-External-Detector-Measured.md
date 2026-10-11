# SL04 — An external content detector, measured before it is adopted

**Status:** Estimated.
**Size:** L - a new detector kind with its own process protocol and pinning, plus a measurement of a
catalog still to be chosen.

**What must be true.** Somebody else's detector catalog can sit behind the filter's detector
interface and is adopted only once a measurement shows it catches something the built-in detectors
do not, without weakening anything the frame promises.

## Why

The envelope checks, the decision model, the answer format and the directory contract are this
repository's own work and are what Sokar leans on. The catalog behind the content checks is not
special: credential patterns age as providers invent new prefixes, and de-obfuscation is a
specialist's game. The interface and the built-in detectors are described in
[`doc/filter.md`](../doc/filter.md) and [`doc/detectors.md`](../doc/detectors.md), the reasoning in
[`doc/decisions.md`](../doc/decisions.md).

## The shape

- **An external implementation is a separate module**, invoked as a subprocess, pinned by version
  and by digest, and every receipt records what it is and which version answered, so a verdict can
  still be explained a year later.
- **It fails closed** like every detector: absent, timed out, crashed or answering outside its
  contract means the message is not accepted.
- **Its findings pass through the frame's redaction** like any other detector's, and it can neither
  release a message nor turn a report into a block.
- **Only single-message detectors are pluggable.** Correlation across messages stays built-in
  ([SL08](README.md#sl08)): measured with the surveyed scanner on a VM, it reassembles a payload split
  over two requests only while one long-running process holds the session, and a process per
  message - how this filter invokes a detector - starts empty every time.

      whole secret, one request                         deny  (the pattern works)
      halves, shared session, one process               allow, then deny as a cross-request fragment
      halves, no session                                allow, allow
      halves, shared session, the server restarted      allow, allow - the first half is forgotten

- **A catalog in another language is allowed**: reusing a maintained catalog beats keeping our own.
  It is pinned, in the bill of materials, and named in `AGENTS.md` with its reason once adopted.

## Candidates

Three catalogs, each open source, maintained, released with versioned binaries or packages that can
be pinned by digest, and able to read a text from standard input; the operator picks the one measured
before it is built.
Versions and digests are read from each project's releases when it is pinned.

| Catalog | Covers | Licence | Upkeep | Pinned as | Against it |
|---|---|---|---|---|---|
| **Gitleaks** | credentials: some two hundred provider rules, with entropy and allow-lists | MIT | very active, frequent releases | one static Go binary per release, with published checksums | credentials only, no personal data; built for repositories, so its rules are tuned for code, not prose |
| **Microsoft Presidio** (analyzer) | personal data: e-mail, phone, card numbers, IBAN and many national ids, with context words and checksums | MIT | active, maintained by a company | a Python package and its language model, pinned by hash in a lock file | Python and a model of several hundred megabytes in the build; slow to start, so a process per text is the question |
| **Yelp detect-secrets** | credentials: a few dozen plugins, keyword and entropy | Apache-2.0 | maintained, slower releases | a Python package, pinned by hash | fewer provider rules than Gitleaks; Python in the build |

**My proposal:** Gitleaks first - one pinned binary, no runtime of its own, and the widest credential
catalog, measured against [SL07](README.md#sl07)'s credential half. Presidio is the one to measure if
the personal-data half is what we want to reuse instead. **Not proposed:** TruffleHog, whose AGPL
licence and network calls to verify a found key do not fit a filter that must not reach out.

## Plan

- **`detect/ExternalDetector`**, a `ContentDetector` whose `inspect` writes the text to a pinned program
  and reads findings back (JSON lines: rule id, category, severity, offsets). Its `id()` carries the
  program's name and version, so `Answers` already records which one answered in a receipt's
  `detectors`.
- **Fails closed through what exists:** absent, timed out, non-zero or unparseable throws, and
  `DetectorRunner` turns that into `FRAME/DETECTOR_FAILED`, which always refuses. Findings go through
  `toFinding`, so redaction and the rule-id check apply unchanged.
- **`Settings`:** `detector.external.command`, `.sha256` and `.timeoutMillis`; the program's digest is
  checked at startup and a mismatch is a `ConfigException`.
- **The measurement:** a rates test beside `EncodedPayloadRatesTest`, against `english-corpus.txt` and
  the fixtures, tagged so it runs only where the program is installed.
- **Red first:** in `MessageSluiceTest`, a stand-in program that hangs, exits 1 and prints garbage -
  each message must not reach `accepted/`.

## Acceptance

- One external implementation behind the interface is **measured** on this repository's fixtures:
  false positives against the clean English corpus, catches against the negative samples per
  category, and the cost per message.
- An implementation that is absent, hangs past its timeout, exits non-zero or prints something
  unparseable **must** each be seen to stop the message from being accepted, asserted on what
  reaches `accepted/`, not on a log line.
- The same fixture through the built-in and the external implementation **must** give findings of
  the same shape, differing only in rule ids and what was caught.

## To be checked

- Whether a process per text is affordable, or the interface needs a per-message call for an external
  detector - measured with the chosen catalog.
