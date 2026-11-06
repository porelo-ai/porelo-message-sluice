# SL38 — Mutation Testing With PIT

**Status:** Analyzed.
**Size:** M - the profile is a few lines in the root pom, but sorting about 340 mutants that no test kills, across 21
classes, is about a day.

**What must be true.** Whoever works on the filter can run PIT by hand and read which checks no test holds: a `pit`
profile, not in CI and with no threshold, with PIT and its JUnit 5 plugin pinned, run on a VM where the tests call real
tools, and a report that sorts the survivors into real gaps and harmless ones. The `pit` profile in
`porelo-message-matrix`'s root pom is the model.

## Plan

Read in today's root `pom.xml`, `filter/app/pom.xml`, `build.md` and the tests, and measured with a probe profile on the
host, from a scratch worktree of `9211910`: PIT 1.30.2 with `pitest-junit5-plugin` 1.2.3, all of
`org.fuin.porelo.msgsluice.filter.*` against every `*Test`, 8 threads on 16 cores, 31 minutes. 1558 mutants: 1024
killed, 192 timed out (counted as detected), 278 survived, 64 not covered; line coverage 92 %. The unit suite alone
takes 18 seconds, 279 tests, none skipped as a non-root user.

- **The profile:** `pit` in the root `pom.xml`, beside the others, as in the model: `pitest-maven` 1.30.2 and
  `pitest-junit5-plugin` 1.2.3 by version (the newest releases on Maven Central today), target classes from a
  `pit.classes` property defaulting to `org.fuin.porelo.msgsluice.filter.*` so one change's classes can be named with
  `-Dpit.classes=...`, target tests `*Test`, `package-info` and the `main` method excluded, HTML and XML reports
  without timestamps, no `mutationThreshold`. It is run as `./mvnw -B -s settings.xml -Ppit test-compile
  org.pitest:pitest-maven:mutationCoverage -pl filter/app -am`; no workflow names the profile.
- **Timeouts:** 192 mutants run into PIT's timeout, mostly in the filter's scanning loops, and they make most of the 31
  minutes. A `timeoutConstant` lower than PIT's 4000 ms default is measured once; it is kept only if the counts stay the
  same.
- **A child JVM runs unmutated code:** `MainTest` starts the filter in a JVM of its own on `java.class.path`; under PIT
  that JVM loads the original classes, so a mutant only that test reaches survives without being a gap. The model
  passes Surefire's class path in a property for the same reason. Such mutants are sorted harmless and named.
- **The run on the VM:** in the `sluice` account, non-root, in a podman container from `porelo.yml`'s image as for a
  workflow run, so `mkfifo`, `bash`, a separate `/dev/shm` and the permission tests are real. The numbers are compared
  with the host's; a difference is explained.
- **The report:** in this issue, one line per surviving or uncovered mutant group, by class: `EncodingSignatureDetector`
  58, `EnvelopeCheck` 49, `Calibration` 35, `MessageSluice` 34, `Masked` 26, `FileOps` 25, `TokenShapeDetector` 21,
  `Answers` 20, `Main` 17, `SshSignature` 13, `Normalizer` 13, `DirectoryLock` 8, `MessageArchive` 7, `EnvelopeResult` 6,
  the rest 1 to 3. Each is a real gap (a check that could change, and no test fails, where a fault does harm: a payload
  let through, a refusal that leaks what it matched, a message lost or filed twice) or harmless (equivalent, logging,
  wording, or reached only through the child JVM), with the reason in a few words.
- **How to run it** goes into `build.md`, a short section beside the native build: the command, `-Dpit.classes`, where
  the report lands, and that it has no threshold and runs in no workflow.
- **No test goes red first:** the profile changes nothing that ships or runs in CI; the real gaps are what goes red,
  in the issues they become.

## Acceptance

- `./mvnw -B -s settings.xml -Ppit test-compile org.pitest:pitest-maven:mutationCoverage -pl filter/app -am` writes
  `filter/app/target/pit-reports/index.html` and `mutations.xml`, on the host and on the VM.
- No workflow runs the profile, and the normal build does not resolve PIT.
- Every surviving and uncovered mutant of the VM run is in the report, sorted, each harmless one with its reason.
- `build.md` says how to run it.

## Open questions

- The real gaps: written as new issues of this repository, one per class or area, each fixed with a test seen to fail
  first (proposed), or fixed inside this issue, which would make it L.
