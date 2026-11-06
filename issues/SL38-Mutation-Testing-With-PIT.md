# SL38 — Mutation Testing With PIT

**Status:** Implementing.
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
- **Timeouts:** about 150 mutants flip between a timeout and survival with the machine's load: they only slow a
  scanning loop, finish, and no test fails. With PIT's default of 4 s they counted as timed out in one run (192) and
  as survivors in the next (39 timed out). The profile sets `timeoutConstant` to 10 s and 4 threads by default, so such
  a mutant finishes and counts as what it is; a run on fewer cores than threads raises the timeout further.
- **A child JVM runs unmutated code:** `MainTest` starts the filter in a JVM of its own on `java.class.path`; under PIT
  that JVM loads the original classes, so a mutant only that test reaches survives without being a gap. The model
  passes Surefire's class path in a property for the same reason. Such mutants are sorted harmless and named.
- **The run on the VM:** in the `sluice` account, non-root, in a podman container from `porelo.yml`'s image as for a
  workflow run, so `mkfifo`, `bash`, a separate `/dev/shm` and the permission tests are real. The numbers are compared
  with the host's; a difference is explained.
- **The report:** under *Report* below, the classes that guard what does harm first, the rest in an issue of its own.
  Each mutant is a real gap (a check that could change, and no test fails, where a fault does harm: a payload let
  through, a refusal that leaks what it matched, a message lost or filed twice), counted under the area's issue that
  names its places, or harmless (an edge, wording, equivalent, failing closed, durability, a configuration check, or
  reached only through the child JVM), counted under its reason with where it lies.
- **How to run it** goes into `build.md`, a short section beside the native build: the command, `-Dpit.classes`, where
  the report lands, and that it has no threshold and runs in no workflow.
- **The real gaps become new issues of this repository**, one per class or area, filed Analyzed and sized, each to be
  fixed with a test seen to fail first; this issue itself is the profile, `build.md`'s lines and the sorted report.
- **No test goes red first:** the profile changes nothing that ships or runs in CI; the real gaps are what goes red,
  in the issues they become.

## Acceptance

- `./mvnw -B -s settings.xml -Ppit test-compile org.pitest:pitest-maven:mutationCoverage -pl filter/app -am` writes
  `filter/app/target/pit-reports/index.html` and `mutations.xml`, on the host and on the VM.
- No workflow runs the profile, and the normal build does not resolve PIT.
- Every surviving and uncovered mutant of the harm-guarding classes is in the report, sorted, each harmless one under
  its reason; the rest is in its own issue. The VM run's counts match the host's, or the difference is explained.
- `build.md` says how to run it.

## Report

The host's run with the `pit` profile as committed, 4 threads and a 10 s timeout: 1557 mutants, 1030 killed, 19 timed
out, 445 survived, 63 not covered, in 18 minutes. Of the 508 left, the 388 in the classes that guard what does harm are
sorted here; the 120 in the others go to [SL48](README.md#sl48).

**Real gaps, 227 mutants, each area an issue of its own:**

| Area | Mutants | Issue |
|---|---|---|
| A refusal's explanation and keys (`DetectorRunner.quotes`, `EnvelopeResult`) | 16 | [SL41](README.md#sl41) |
| The envelope's walk of a task, artifact, part, extensions, A2A 0.3 (`EnvelopeCheck`) | 38 | [SL42](README.md#sl42) |
| Filing, names, signatures, size limit, directory identity (`FileOps`, `MessageSluice`) | 24 | [SL47](README.md#sl47) |
| What masking takes out (`Masked`) | 43 | [SL43](README.md#sl43) |
| Encoding signatures and their prose exemptions (`EncodingSignatureDetector`) | 49 | [SL44](README.md#sl44) |
| Invisible characters and look-alikes (`Normalizer`) | 9 | [SL45](README.md#sl45) |
| Token shape, entropy, letter distribution (`TokenShapeDetector`, `Tokens`, `EntropyDetector`, `DistributionDetector`) | 48 | [SL46](README.md#sl46) |

**Harmless, 161 mutants:**

| Why | Mutants | Where |
|---|---|---|
| An edge: `>` for `>=` on a measured threshold or a limit, one character or a rounding apart | 80 | detectors, `Masked`, `Normalizer`, `Tokens`, `Redactor`, the envelope's limits, `FileOps.read`'s last check |
| Wording: a detector's id or label, a count in an explanation, a log line, a note, a version | 54 | every detector, `MessageSluice`, `EnvelopeCheck.where`, `FileOps`' deleted-or-not result, which only picks a note |
| Equivalent: the outcome is decided elsewhere first (a blocking finding before the weighing, offsets checked twice) | 12 | `DecisionModel`, `DetectorRunner.toFinding`, `Normalizer`, `Masked.source`, `PersonalDataDetector` |
| Fails closed: the mutant refuses or hides more | 6 | context windows of credential and personal-data rules, `Redactor`'s stars, `EnvelopeResult.under`, a wider look-alike span |
| Durability or an interrupt flag, seen only after a crash or an interrupt | 4 | `FileOps.force`, `Thread.interrupt` |
| A detector's own argument check, which guards a configuration, not a message | 5 | constructors of three detectors |

The sorting reads each place in the code; a harmless one becomes a gap when a test of the issue above shows harm.
