# Changelog

All notable changes to this project are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Fixed

- The `Shared rules check` fetches the pinned `shfmt`, which the style check needs for the two scripts in
  every Maven run.

### Changed

- The build takes `porelo-parent` 0.2.1-SNAPSHOT and `porelo-buildtools` 0.5.1-SNAPSHOT. `validate` checks
  the shared code style - `google-java-format` in its AOSP style for Java, `shfmt` for the scripts - and the
  code is formatted to it. The workflows check out without keeping the job's token.

### Fixed

- An id is checked as text even when the envelope refuses a message before the detectors run, so a payload
  sent as an id never comes back in the answer.
- A message a crash left in `incoming/` after it was filed is finished on the next pass, not refused as its own
  duplicate. A message and its signature are removed only together, so a signature is never left behind for
  the next message of its name.
- `--watch` no longer ends when Porelo moves a file on while the archive is read, and ends with 2 when its lock
  file was removed or replaced, before a second instance could file the same messages.
- Names in capitals and digits joined by dashes, such as `A2A-In` or `E2E-Run1`, pass as prose instead of
  being refused as base64.
- A pass started through the Java API checks the directories when nobody did first.
- The Build runs a signed message, and one signed in the old namespace, through both packages.

## [0.5.0] - 2026-10-17

### Changed

- Sokar is now Porelo: the filter is `porelo-message-sluice-filter`, published as `org.fuin.porelo` 0.5.0.
  Its packages install it at `/usr/libexec/porelo/`, depend on the package `porelo` and are published to
  `porelo-dist-deb` and `porelo-dist-rpm`; `install-local.sh` puts it in `$XDG_DATA_HOME/porelo/filter/` and
  removes nothing else. A signature beside a message passes only in the namespace `porelo-message`, the one
  Porelo writes.
- Every workflow run is titled by its workflow, branch and commit, and the shared-rules workflow is named
  `Shared rules check`.
- The `Shared rules check` runs once a day, on a pull request and by hand, no longer on every push; the
  documents are checked on the host before every hand-over.
- The build takes `porelo-parent` 0.2.0, and with it `porelo-buildtools` 0.5.0.

## [0.4.1] - 2026-10-08

### Changed

- The documents are checked by the shared `check-citations` and `check-doc-site` of `sokar-release` 0.4.1, with
  the tests that read documents, in a `Shared rules` workflow on every push and pull request; the build skips a
  change to documents alone, and its own citation test is gone.
- A release refuses to build when anything it builds or packages with is a snapshot, as `check-releases` of
  `sokar-release` 0.4.2 reads it from the effective pom.
- Every module has a `README.md` that links its submodules, and the `Shared rules` workflow holds it with
  `check-readmes` of `sokar-release` 0.4.2.
- The build takes `org.fuin.sokar:sokar-parent` as its parent instead of `org.fuin:pom`, and drops what it gave
  with the same values - the Java release, the plugin and tool versions, the `ci-tools` profile, publishing
  nothing, the NullAway setup of the main compile; the packages are the same, and the tests run on JUnit
  5.14.2, the parent's, instead of 5.12.2.
- The `.rpm` module reads the version back out of the built `.deb` and `.rpm` (`PackageVersionIT`) and fails
  the build when either does not carry the project's version as a package version (a snapshot as
  `~snapshot.<run>`, worked out by the test, not taken from the build), or the `.rpm`'s release is not 1.
- The build takes `sokar-parent` 0.1.2, and with it `sokar-buildtools` 0.4.3.

## [0.4.0] - 2026-10-05

### Added

- Initial public version.
