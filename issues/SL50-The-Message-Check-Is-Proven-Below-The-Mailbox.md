# SL50 — The Message Check Is Proven Below The Mailbox

```json
{
  "$schema": "issue-1.json",
  "id": "SL50",
  "title": "The Message Check Is Proven Below The Mailbox",
  "state": "Analyzed",
  "size": "L",
  "priority": "Medium",
  "rank": 1,
  "blockedBy": [],
  "covers": "The test pyramid audited: 157 unit, 129 integration, 5 end-to-end runs; the envelope check, the answer and the per-file decision proven only through a real mailbox, moved to unit tests over fakes, and a watch's end tested without a clock.",
  "openQuestions": 1
}
```

**Why this size.** Three fixes, M, S and S, each one commit in `filter/app` alone, no contract touched.

**What must be true.** The filter's tests follow the test pyramid: what a message may contain, what its answer may
say and when a watch ends are proven by unit tests over fakes in milliseconds, and only what the real filesystem, a
child process, the native binary or the packages alone can show is tested against them.

## Why

Measured on `d88b0b2` with `./mvnw -B -s settings.xml -Pnative,dist clean verify` on the host (16 cores), and the
workflow's step *Run the packaged binaries against the fixtures* run from a copy extracted verbatim. A test counts as
unit when it exercises one class and touches nothing outside it but its own test resources; as integration when it
runs against a real directory tree, a child process (`bash`, `java`, `mkfifo`), the native binary or the packages; as
end-to-end when the packaged product runs as Porelo meets it.

| Level | Suites | Tests | Time |
|---|---|---|---|
| Unit | the `detect` tests, `DecisionModelTest`, `RedactorTest`, `SshSignatureTest`, `SettingsTest`, `NullMarkedPackagesTest`, `WorkflowLimitsTest`, `ModulesDescribedTest`, `DocumentTestsTaggedTest` | 157 (54 %) | 16.3 s, of it 10.4 s `EncodedPayloadRatesTest` |
| Integration | `MessageSluiceTest` (63), `cli.MainTest` (18), `WorkflowCommitsTest` (17), `WorkflowTokensTest` (7), the `io` tests (13), `CalibrationTest`, `FixturesTest` (2 each); failsafe: `LinkageIT` (3), `PackageVersionIT` (2 per package) | 129 (44 %) | 5.7 s surefire, 0.5 s failsafe, after a native build of 1 min 54 s |
| End-to-end | the workflow's fixture step, two mailbox runs per package format; `buildtools/acceptance.sh` on a rented machine | 5 runs (2 %) | 0.6 s for the four fixture runs; `acceptance.sh` not run here |

The unit suite runs in 22 s, under a minute. The unit share, 54 %, is below the pyramid's 70-80 %, and nearly all
the excess of integration sits in `MessageSluiceTest`.

**Flows proven only through a real mailbox directory or a child process:**

- **The envelope check** - `a2a.EnvelopeCheck`, `A2aReader`, `MessageFacts`, `Unprocessable`: roles, ids, peer
  names, unknown fields and their depth and size, a part with text and raw, broken JSON. No test of the `a2a`
  package exists; `MessageSluiceTest` builds an `EnvelopeCheck` directly twice and otherwise reaches it by
  delivering files and reading where they land.
- **The answer** - `Answers`: that a refusal never carries what it refused, keys named by position only, broken JSON
  answered with name and hash only, every answer reading back through the check. Proven only by reading files from
  `feedback/`.
- **The per-file decision** - `MessageSluice.process` and `checkName`: the outcome from envelope, detectors and mode,
  the file-name check, a signature paired with its message. 270 lines reached only through a pass over a real
  directory.
- **The agent card** - `AgentCard`: only through `cli.MainTest`.

About 30 of the 63 cases in `MessageSluiceTest` guard the decision, not the filing, among them
`aPartWithTextAndRawIsRefusedEvenWhileOnlyReporting`, `anUnknownFieldIsHeldToTheSameDepthAndSizeAsMetadata`,
`aPeerNamedAsATaskOfAnotherProjectIsAcceptedAsPoreloNamesIt`, `aRejectionNeverCarriesWhatItRefused`,
`aSecretInBrokenJsonIsAnsweredWithNothingButNameAndHash`, `reportingModeAcceptsAndNamesWhatBlockingModeRefuses`,
`aPersonMayWriteButARoleNobodyHasIsRefusedAndAUserInATasksHistoryIsFine`,
`everyIdIsCheckedAsTextAndAnIdThatIsAPayloadIsNeverEchoed`, `aKeyARuleMatchedIsNamedByItsPositionOnly`,
`aSecretInAPlainFileNameIsRefusedLikeOneInATextAndNeverRepeated` and
`onlyTheSignaturePoreloWritesTravelsBesideAMessage`.

**Tests that wait:** `watchingRunsAPassWhenAFileArrivesAndStopsWhenInterrupted`, `aPassThatFailsOnADirectoryEndsTheWatch`
and `aWatchWhoseLockWasRemovedEnds` start a real thread, let it poll every 50 ms and join it for up to 10 s.

**Kept as they are, each for a reason:** no test reaches the network (the workflow tests run the steps' own shell with
`git` and `java` stubbed and server URLs as plain strings). The `io` tests stay on a real directory tree, since links,
owners, modes, a FIFO and a move across filesystems are what they guard. `anInheritedEnvironmentCannotLetAPayloadThrough`
starts its own JVM because only `main()` sees the real environment. The two-second `assertTimeoutPreemptively` guards in
`CredentialsTest` and `PersonalDataTest` guard against a hang. `EncodedPayloadRatesTest` measures the rates over the
documentation corpus in 10.4 s, and the unit suite stays under a minute with it.

## Plan

- **F1 (M): the per-message check without the mailbox.** The part of `MessageSluice.process` and `checkName` that
  turns a name, the bytes and an optional signature into an outcome, findings and an answer moves into a class that
  touches no file; `MessageSluice` keeps reading, filing, links, permissions, the lock and recovery. The about 30
  decision cases of `MessageSluiceTest` move to unit tests of that class over stub detectors; `MessageSluiceTest`
  keeps the cases whose harm lies in filing. Red first: a unit test of the new class against a role check broken on
  purpose. The new tests of [SL41](README.md#sl41) and [SL42](README.md#sl42) go to this level.
- **F2 (S): the envelope and the answer tested on their own.** Unit tests of `EnvelopeCheck` and `A2aReader` for
  each refusal reason and limit, and of `Answers` for what an answer may carry, each built from a string, with no
  directory.
- **F3 (S): a watch's end without a clock.** `watch` waits for the next event through a small injected interface
  instead of polling its `WatchService` directly, as the stability delay already waits through `MessageSluice.Pause`;
  its three ends - interrupted, a failed pass, a lost lock - become unit tests with a wait that returns at once. One
  test against the real `WatchService` and thread stays.

## Acceptance

- The unit share, counted as above, is at least 70 %, and the unit suite stays under a minute.
- Every flow named under *Why* has unit tests that fail when its rule is broken on purpose, seen red.
- No test but the one real-thread watch test waits on a thread, a poll or a timeout to see a flow work.

## Open questions

- One issue or three: F1 (M), F2 (S) and F3 (S) can be built and handed over apart, as three issues, or together as
  this one.

## To be checked

- The counts under *Why* recounted on the commit the build starts from: the Backlog issues before it add tests to
  `MessageSluiceTest` and the detector suites, which shifts the shares.
