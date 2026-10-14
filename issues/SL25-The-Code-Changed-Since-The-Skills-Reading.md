# SL25 — The Code Changed Since The Skills Reading

**Status:** Analyzed.
**Size:** M - about 1,400 changed lines in one module, its packaging and three scripts; each finding
needs its own test seen red, and a fix to what ships runs the acceptance on the VM again.

**What must be true.** The code changed since the last reading against this repository's skills has
been read against them too, so every line of the filter that ships has had that second author.

## Why

The whole code was read against the seven skills on 2026-09-27 (closed in `ddea1c3`, kept on
the local branch `pre-public`). Everything since - the split-payload correction, the null checks, the
workflow limits, the RPM packaging, the transports' removal - was written and tested by one author
only. The shared rules now ask for each issue's code to be read before its hand-over; this issue
catches up what came before that rule.

## Plan

- **The scope is `git diff ddea1c3 main -- filter install-local.sh buildtools .github`**, read on
  the code as it stands then, not on the diff alone: 39 files under `filter/`, `install-local.sh` and
  `buildtools/` (981 lines added, 419 removed) and the five files under `.github/`.
  - `filter/app/src/main`: `MessageSluice`, `Settings`, `Calibration`, `cli/Main`,
    `a2a/EnvelopeCheck`, `a2a/EnvelopeResult`, `detect/EncodingSignatureDetector` (167 lines changed,
    the most), `detect/EntropyDetector`, `io/FileOps`, `io/MessageArchive`, `io/SshSignature`.
  - `filter/app/src/test`: the tests beside them, and the new `DocumentTestsTaggedTest`,
    `NullMarkedPackagesTest`, `WorkflowLimitsTest`, `detect/SplitPayloadTest`.
  - The packaging: `filter/dist-rpm/pom.xml`, `filter/dist-deb/src/deb/control`, the poms.
  - The scripts and workflows: `install-local.sh`, `buildtools/acceptance.sh`, `.github/`.
- **Which skill reads what:** all seven on `src/main`; `test-quality`, `java-code-review` and
  `clean-code` on `src/test`; `graal` on the `native` profile and the packaging; `security-audit` on
  the scripts, the workflows' permissions and the package files, and weighing heaviest on `io/` and
  `MessageSluice`, where a refusal must not exfiltrate what it prevents and the filter fails closed;
  `concurrency-review` on `DirectoryLock`'s users and the archive.
- **The skills, as the storage API gave them on 2026-10-14**, each download checked against its
  SHA-256 before it is loaded, and read again when the reading starts:

      graal               2026.910.3  b24ec6272fb13490e32977c21d4543869a7e85ed474fdc0ff34022fbddd88bf6
      java-code-review    2026.906.3  802f86339442ed9fa9d5bca14b6d16e1f6420ae92a047b18bba11239e32c698b
      test-quality        2026.906.3  9c5d23ad7a6d9c6f5c3679f3cc44a7fcc2fb3cc1c0f0d0ba4a2c0f7812428bad
      security-audit      2026.906.3  a46fd1be07bd539b955291537dea0f24f65a2f8b15d40d50a6a9098bbd992747
      concurrency-review  2026.906.3  3a0aab612f25483a3fe777eb18fbcc434e936b19298708bf88beff8b22d79e56
      clean-code          2026.906.3  5cf777a24630298b4b801c934f599722978c21aeac53d61683243908fc041c7b
      solid-principles    2026.906.3  bd47cdab9126dde76cf1703472167a1c88c85306a4e0263a8f2db4c462740cf5

- **Red first:** each finding gets a test that fails on today's code before it is fixed; a finding
  in a script gets a case in `buildtools/acceptance.sh` or a test that reads the script.
- **A *Findings* section** is added here as the reading goes: per skill, each finding with its file
  and line and its outcome - fixed in which commit, or declined with the reason.
- **Starts after Phase 0's rename and code style window has closed**, on the renamed, reformatted
  code; the scope is then the same files under their new names.

## Acceptance

- Every finding is fixed with a test seen to fail without the fix, or declined with its reason here.
- A skill that found nothing says so, by name.
- A skill a measurement contradicts is written down beside the measurement, in `doc/decisions.md` if
  it settles something.
- The hand-over names each skill with the version it was read in, and the whole workflow is green on
  the VM, with the counts of tests run and skipped.

## To be checked

- Whether the harness loads the reviewed packages from its skills directory, or each `SKILL.md` is
  read from outside it.
