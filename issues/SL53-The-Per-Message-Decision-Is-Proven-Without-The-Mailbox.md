# SL53 — The Per-Message Decision Is Proven Without The Mailbox

```json
{
  "$schema": "issue-1.json",
  "id": "SL53",
  "title": "The Per-Message Decision Is Proven Without The Mailbox",
  "state": "Estimated",
  "size": "M",
  "priority": "Medium",
  "rank": 4,
  "blockedBy": [],
  "covers": "The part of `MessageSluice` that turns a name, the bytes and a signature into an outcome and an answer becomes a class that touches no file; the about 30 decision cases of `MessageSluiceTest` move to unit tests over stub detectors."
}
```

**Why this size.** One new class carved out of `MessageSluice` and about 30 test cases moved, in `filter/app` alone,
no contract touched.

**What must be true.** What a message may contain and what its answer says are proven by unit tests over fakes in
milliseconds; only what the real directory tree can show - filing, links, permissions, the lock, recovery - is
tested against one.

## Why

Measured on `d88b0b2` with `./mvnw -B -s settings.xml -Pnative,dist clean verify` on the host (16 cores). A test
counts as unit when it exercises one class and touches nothing outside it but its own test resources; as integration
when it runs against a real directory tree, a child process, the native binary or the packages; as end-to-end when
the packaged product runs as Porelo meets it. Unit 157 tests (54 %, 16.3 s), integration 129 (44 %), end-to-end 5
runs (2 %). Nearly all the excess of integration sits in `MessageSluiceTest` (63 cases).

`MessageSluice.process` and `checkName` - the outcome from envelope, detectors and mode, the file-name check, a
signature paired with its message - are 270 lines reached only through a pass over a real directory. About 30 of
the 63 cases in `MessageSluiceTest` guard the decision, not the filing, among them
`aPartWithTextAndRawIsRefusedEvenWhileOnlyReporting`, `anUnknownFieldIsHeldToTheSameDepthAndSizeAsMetadata`,
`aPeerNamedAsATaskOfAnotherProjectIsAcceptedAsPoreloNamesIt`, `aRejectionNeverCarriesWhatItRefused`,
`aSecretInBrokenJsonIsAnsweredWithNothingButNameAndHash`, `reportingModeAcceptsAndNamesWhatBlockingModeRefuses`,
`aPersonMayWriteButARoleNobodyHasIsRefusedAndAUserInATasksHistoryIsFine`,
`everyIdIsCheckedAsTextAndAnIdThatIsAPayloadIsNeverEchoed`, `aKeyARuleMatchedIsNamedByItsPositionOnly`,
`aSecretInAPlainFileNameIsRefusedLikeOneInATextAndNeverRepeated` and
`onlyTheSignaturePoreloWritesTravelsBesideAMessage`. The agent card, `AgentCard`, is reached only through
`cli.MainTest`.

## Plan

- The part of `MessageSluice.process` and `checkName` that turns a name, the bytes and an optional signature into an
  outcome, findings and an answer moves into a class that touches no file; `MessageSluice` keeps reading, filing,
  links, permissions, the lock and recovery.
- The about 30 decision cases of `MessageSluiceTest` move to unit tests of that class over stub detectors;
  `MessageSluiceTest` keeps the cases whose harm lies in filing. `AgentCard` gets a unit test of its own.
- Red first: a unit test of the new class against a role check broken on purpose.
- The new tests of [SL41](README.md#sl41) and [SL42](README.md#sl42) go to this level.

## Acceptance

- Each moved case fails as a unit test when its rule is broken on purpose, seen red.
- The unit share, counted as under *Why*, is at least 65 % (30 cases moved from 129 to 157 give 187 of 286), and
  the unit suite stays under a minute.

## Decided by the operator (2026-11-12)

- The test pyramid audit is built as three issues: this one, [SL54](README.md#sl54) for the envelope and the answer,
  and [SL55](README.md#sl55) for a watch's end.

## To be checked

- The counts under *Why* recounted on the commit the build starts from: the Backlog issues before it add tests to
  `MessageSluiceTest` and the detector suites, which shifts the shares.
