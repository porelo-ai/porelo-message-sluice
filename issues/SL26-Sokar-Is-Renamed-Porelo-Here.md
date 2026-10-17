# SL26 — Sokar Is Renamed Porelo Here

**Status:** Review.
**Size:** S - one script run over 144 files, one signature fixture made again, and the build, the
documents and the acceptance on the VM; every contract here only follows one Sokar renames first.

**What must be true.** Nothing this repository builds, installs or documents still says Sokar, except
where it says what the old name was: the filter is `porelo-message-sluice-filter`, installed where
Porelo looks for it, and it accepts the signature Porelo writes.

From sokar-project [PJ41](https://github.com/sokar-ai/sokar-project/blob/main/issues/README.md#pj41),
which decides the names, the order and that nothing is migrated.

## What carries the name here

Counted on `main` at `04a7bfe`: 664 occurrences of `sokar` in any case, in 105 of 144 tracked files,
and 73 paths - the 72 Java files under `org/fuin/sokar/msgsluice/` and the signature fixture.

Contracts something outside this repository reads or writes, each renamed on purpose:

- **Maven:** the group `org.fuin.sokar`, the artifacts `sokar-message-sluice`,
  `sokar-message-sluice-filter-parent`, `sokar-message-sluice-filter`, `-deb` and `-rpm`; the parent
  `sokar-parent`; the tools `sokar-release` and `sokar-machines` in the workflows; the Java packages
  `org.fuin.sokar.msgsluice.*`.
- **What Porelo reads:** the binary `sokar-message-sluice-filter`, at `/usr/libexec/sokar/` from the
  packages and at `$XDG_DATA_HOME/sokar/filter/` from `install-local.sh`; the packages' dependency
  on the package `sokar`.
- **What Porelo writes:** the signature beside a message, checked for the SSHSIG namespace
  `sokar-message` (`SshSignature.NAMESPACE`). Sokar signs with it, so `porelo-message` is renamed
  in `sokar` in the same window, or every signed message is refused.
- **The forge and Artifactory:** `github.com/sokar-ai/sokar-message-sluice` in the poms, the README,
  `build.md` and the Debian control file; the site `sokar-ai.github.io/sluice/`; the repositories
  `sokar-dist-deb` and `sokar-dist-rpm` the Build uploads to and reads back.

## Exceptions, put back after the replacement

- **`CHANGELOG.md`'s released entries**, 0.4.0 and 0.4.1, as released.
- **`filter/app/src/test/resources/english-corpus.txt`**, the frozen corpus the encoding rates are
  measured on (95 occurrences); it is a measurement, not a description, and is exempt from the
  citation check for the same reason.
- **The 0.5.0 changelog entry and the decision that record the rename**, which name Sokar on
  purpose.

## What the script cannot do

- **The signature fixture** `sokar-ed25519.sig` holds the namespace inside its base64, under a
  signature: it is made again with a new throwaway key, `ssh-keygen -Y sign -n porelo-message`, as
  its `README.md` says, as `porelo-ed25519.sig`. The longer namespace makes it 310 bytes, not 306,
  so the size in `SshSignature`, its test and the fixture's `README.md` moves with it. The old one
  stays, as `old-namespace-ed25519.sig`, for the tests that refuse it.
- **`install-local.sh` removes leftovers of Sokar's retired transports and old jars.** With no
  migration there is nothing of Porelo to remove there, so those lines are deleted, not renamed.
- **Lengths:** `porelo` is one character longer. No test here asserts a cut or a padded line, but
  `EncodingSignatureDetectorTest` feeds `SOKAR_E2E_HOST` and `SOKAR_E2E_KEY` lines to a detector
  whose scores depend on the text, so those cases are run, not assumed.
- **The Debian pool's letter:** the Build uploads to and reads back from `pool/main/s/<package>/`
  (three places in `build.yml`, one in `build.md`), the first letter of the package's name; it
  becomes `pool/main/p/`, by hand, since the script replaces only the word.
- **No image** in this repository draws the name.

## Plan

1. **Red first:** a test tagged `documents` beside `DocumentTestsTaggedTest`, failing while any
   tracked file outside the exceptions says `sokar` in any case, a path included; seen red on
   today's tree, green after the script, and kept.
2. **The script** in the window, after `porelo-parent`, `porelo-buildtools` and `porelo` are
   published as snapshots: `git mv` of `org/fuin/sokar` and the fixture, the three spellings
   replaced, the exceptions put back, the version moved to `0.5.0-SNAPSHOT`, the parent to
   `porelo-parent` `0.2.0-SNAPSHOT`. The script is reviewed; the commit holds nothing made by hand
   but the fixture and its size, the deleted lines of `install-local.sh`, the Debian pool's letter,
   the Shared Area's markers taken from `porelo-buildtools`, the two tests that refuse the old
   namespace, the changelog entry and this issue's state.
3. **Checked as any change:** the whole build with the native profile, the documents and the shared
   checks on the commit checked out, and `build.yml`'s steps on the Ubuntu VM against Porelo's
   packages from `porelo-dist-deb`, with a signed message passing.

## Acceptance

- The name test was seen red before the script and is green after, naming each file it found.
- A message signed by Porelo with namespace `porelo-message` passes; one signed with
  `sokar-message` is filed as unprocessable.
- The Build is green under the new coordinates and publishes `porelo-message-sluice-filter` to
  `porelo-dist-deb` and `porelo-dist-rpm`.
- The unreleased changelog line says what the build takes now, `porelo-parent` 0.2.0-SNAPSHOT and
  `porelo-buildtools` 0.5.0-SNAPSHOT; the lines naming `sokar-release` 0.4.2 are in the released
  0.4.1 entry and stay as released.
- The encoding rates do not move: `english-corpus.txt` is untouched, and `EncodedPayloadRatesTest`
  passes on it with its thresholds unchanged.
