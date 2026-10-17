# SL25 — The Code Changed Since The Skills Reading

**Status:** Backlog. Blocked by [SL27](README.md#sl27), the reformatting.
**Size:** L - about 1,400 changed lines in one module, its packaging and three scripts, read into 16
fixes; each needs its own test seen red, several a seam in `io/` first, one a fixture step in the
Build, and a fix to what ships runs the acceptance on the VM again. M before the reading, set to L at
the operator's word.

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
- **`shellcheck` on each tracked `*.sh`**, as the operator decided: `install-local.sh` and
  `buildtools/acceptance.sh`, both `bash`. Each finding is fixed in the cleanup commit, with a test seen
  red first where one can be. A finding left as it is gets `# shellcheck disable=<code>`, with the
  reason on the line above.
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
- **Starts after the reformatting** ([SL27](README.md#sl27)), on the renamed, reformatted code; the
  scope is then the same files under their new names, and the findings go in one commit after the
  reformatting one.
- **No interface another repository reads changes:** the directory contract, the message format,
  the feedback files, the command line Porelo calls, or the package names and paths. A finding that
  would change one is not built here; it is named in the channel.

## Findings

Read on `main` at `04299f1`, against the seven skills above, each zip's SHA-256 matching the table and
the storage API; none is newer. The harness cannot install them here, so each `SKILL.md` was read from
outside the repository. The reading ran in three read-only sub-agents, at the operator's word of
2026-10-17. `shellcheck` is 0.11.0. Each finding is a guess until its test is seen red, and none is
fixed before [SL27](README.md#sl27). Paths are under `filter/app/src/main/java/org/fuin/porelo/msgsluice/filter/`
unless they say otherwise.

**To fix.** Under the shared rule that a test guards harm, each test names the harm it prevents;
a finding that names none is fixed without a test, and a test that cannot fail and guards none is
deleted, not repaired.

1. **`security-audit`: an id is echoed unchecked when the envelope refuses before the detectors run**
   (`MessageSluice`, around line 400, with `a2a/EnvelopeResult`). A `raw` part, `ROLE_UNSPECIFIED` or
   too many parts are blocking envelope findings, so the detectors never see the ids. Then
   `echoable` hides only an id some finding names. A payload as `messageId` goes back in the answer as
   `rejectedMessageId` and `inReplyToMessageId`, and the unprocessable path, which does check it, shows
   the rule. The fix runs the detectors on the ids in that case too; the answer keeps its fields.
   Red: `MessageSluiceTest`, a 128-hex `messageId` beside a `raw` part, no window of it in anything that
   leaves.
2. **`concurrency-review`: a crash between filing and deleting turns into a contradiction**
   (`MessageSluice`, around lines 394 and 421). Killed after the copy in `accepted/` and before the
   original in `incoming/` is deleted, the next pass sees the id in the archive and refuses it as
   `DUPLICATE_MESSAGE_ID`. It writes "nothing of it was sent" while the accepted copy goes out. The fix
   takes the same bytes under the same name in `accepted/` as already filed. Red: the same bytes put
   in both directories, then one pass, and nothing in `rejected/`.
3. **`concurrency-review`: a file moved away during the archive's scan stops the filter**
   (`io/MessageArchive`, around line 70). Porelo moves files from `accepted/` to `sent/` while
   `--watch` runs. A `NoSuchFileException` between the stamp and the read ends the pass and the
   watch with exit 2. The fix skips the file: the scan reads `sent/` after `accepted/`, so the
   message is still counted. Red: a seam between stamp and read that removes the file.
4. **`concurrency-review`: a lock whose file was removed is not held** (`io/DirectoryLock`, used by
   `cli/Main`). The lock is on the inode. Once `filter/.lock` is deleted, a second instance locks a
   new file, and both file the same message, once as `m.json` and once as `m-1.json`. The fix
   compares the held file with the path after locking and before each pass. Red:
   `DirectoryLockTest`, lock, delete the file, and a second lock is refused.
5. **`java-code-review`: one prose pattern never matches** (`detect/EncodingSignatureDetector`,
   `NAME_SEGMENT`). In `(?=[A-Z0-9]*+[AEIOUY])` the possessive `*+` takes the vowel too, so `E2E`,
   `A2A` and `PORELO` never count as a name. Confirmed in `jshell`: `false` with `*+`, `true` with
   `*`. Joined names like `A2A-In A2A-Out ...` are then refused as base64 cut into pieces. It fails
   closed, but it refuses prose. Red: `EncodingSignatureDetectorTest`, that line of names passes.
6. **`solid-principles`: a message and its signature can come apart** (`MessageSluice`, around line
   427, against `io/FileOps.keepAsRead`). If the `.sig` changed after it was read, the message is
   deleted and the `.sig` stays in `incoming/`. A later message of that name is then filed with it.
   If the message changed and the signature did not, the signature is deleted and the message is
   later filed unsigned. The fix keeps the pair together in one place. Red: a detector that rewrites
   the `.sig` during the check, and afterwards no `.sig` without its message in `incoming/`.
7. **`solid-principles`: `runOnce()` checks no directory unless `checkDirectories()` ran first**
   (`MessageSluice`). `cli/Main` always calls it first, so only a caller of the Java API skips the
   checks. The fix lets `runOnce()` check them when nothing was checked. Red: `rejected/` at 0777, and
   `runOnce()` throws.
8. **`test-quality`: the link test plants its link where nothing writes** (`io/FileOpsTest`). The
   temporary file is named with a UUID, so the planted link is never opened. With `CREATE_NEW`
   replaced by `CREATE, TRUNCATE_EXISTING` the test stays green. Red: that mutation, against a test
   that writes through a temporary name it knows.
9. **`test-quality`: four checks of the signature's shape have no test** (`io/SshSignatureTest`,
   against `io/SshSignature`): the version, the empty reserved string, and nothing after the key or
   the signature inside their strings. Each can be removed and every test stays green, and a payload
   would then fit in the reserved string. Red: one blob for each, each refused.
10. **`test-quality`: the guard for documents tests misses a path** (`DocumentTestsTaggedTest`).
    `DOCUMENT_PATH` matches `filter.md`, not `doc/filter.md`. It guards where a test runs, not a
    harm, so the pattern is widened without a test of its own.
11. **`test-quality`: "one kibibyte everywhere" checks only the constant** (`io/SshSignatureTest`).
    It asserts that a constant equals the number it is set to, and guards no harm: the limit itself
    is held by the tests that refuse a longer signature. It is deleted.
12. **`test-quality`: the FIFO test has no time limit** (`io/MessageArchiveTest`). If the scan
    opened special files again, the test would hang until the job's limit, not fail. Red: the
    mutation, with `@Timeout`.
13. **`graal`: no signed message runs through the packaged binaries** (`.github/workflows/build.yml`,
    the fixture step). The namespace and the pairing of signatures are tested on the JVM only. A
    fixture with `porelo-ed25519.sig` and one with `old-namespace-ed25519.sig` go into that step,
    checked by where they are filed. Red: the step on today's fixtures does not see a signature.
14. **`security-audit`: the checkouts keep their token** (`.github/workflows/build.yml` and
    `shared-rules.yml`). Neither sets `persist-credentials: false`, and no step here pushes with git.
    Red: the shared `check-actions` that refuses it.
15. **`shellcheck`: `buildtools/acceptance.sh:47`, SC2012**, `ls | wc -l` to count the answers. It
    counts files the filter names itself, so it names no harm. It becomes `find`, without a test.
16. **`clean-code`: a Javadoc without its field** (`MessageSluice`, above `FILE_NAME_LOCATION`), left
    from the signature limit's move. It is removed; no test can fail on a comment.

**Not built here, since each would change what another repository reads:**

- **`clean-code`: modes that cannot go together are not refused** (`cli/Main`). `--agent-card`
  with `--calibrate`, `--dry-run` or `--watch`, `--calibrate` with `--file`, and `--dry-run` with
  `--watch` all run the first mode and drop the rest. Refusing them changes which command lines are
  accepted, so it is [SL28](README.md#sl28), an issue of its own.

**Read and left, with the reason:**

- **A FIFO renamed over a message between the stamp and the read** would hang the open. Only a
  writer of `incoming/` can do it, the result is that this mailbox stops and nothing passes, and Java
  opens no file with `O_NONBLOCK`.
- **The workflow's tool classpath** (`-Pci-tools -U`) resolves `porelo-release` and
  `porelo-machines` from the parent's versions, in the job that later holds the Artifactory token. That
  is the shared tooling's, not this repository's.

**Each skill found something:** `security-audit` 2, `concurrency-review` 3, `java-code-review` 1,
`solid-principles` 2, `clean-code` 2, `test-quality` 5, `graal` 1, `shellcheck` 1.

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
