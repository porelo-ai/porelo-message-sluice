# SL59 — The Maven Cache Follows What Was Resolved

```json
{
  "$schema": "issue-1.json",
  "id": "SL59",
  "title": "The Maven Cache Follows What Was Resolved",
  "state": "Implementing",
  "size": "S",
  "priority": "High",
  "rank": 1,
  "blockedBy": [],
  "covers": "The Build and the Shared rules check save the Maven repository whenever its content changed. Today their key hashes only this repository's poms, and an exact hit is never saved over, so what the parent snapshot and its BOMs move to is fetched from Maven Central on every run."
}
```

**Why this size.** Two workflows, `build.yml` and `shared-rules.yml`, change, with the test that reads them. No
contract with another repository changes. It is under an hour of measured wall time.

**What must be true.** A Build whose poms did not change resolves nothing from Maven Central that an earlier Build
already fetched, so a refusal such as `429 Too Many Requests` cannot fail a Build that changed no dependency.

## Why

Both workflows cache the local Maven repository with `actions/cache` under the key
`maven-${{ hashFiles('**/pom.xml') }}`. The versions resolved come largely from `porelo-parent`, a
`0.2.2-SNAPSHOT`, and the BOMs it imports, and those move without any pom here changing. After an exact hit,
`actions/cache` saves nothing, so whatever they moved to is downloaded again on every run until a pom here changes.

Measured: the key form and the parent version, read in `.github/workflows/build.yml`,
`.github/workflows/shared-rules.yml` and `pom.xml`. No version range was found in this repository's poms.
Inferred, not measured: the downloads after a hit. The run logs need a token to read, and no Build ran in the last
7 days.

## Plan

- Split the cache step into `actions/cache/restore` with the prefix `maven-` and `actions/cache/save` after the
  build.
- The save key is a digest of what the repository holds after the build: the sorted list of paths and sizes under
  the local Maven repository, skipping `*.lastUpdated`, `_remote.repositories` and `resolver-status.properties`.
- A Build that fetched nothing finds that key already present and saves nothing. One that fetched something saves a
  new entry, and the next Build restores it through the prefix.
- The same in `shared-rules.yml`.
- Red first: a test in `WorkflowSteps`/`WorkflowLimitsTest` that fails while a Maven cache key is computed from the
  poms alone or the save step is missing.

## Acceptance

- The workflow test fails on today's two cache steps and passes on the new ones, seen red.
- Two Builds run on GitHub one after the other with no pom change. The second restores the first's entry, its log
  has no `Downloading from central`, and it saves nothing.

## To be checked

- How large one entry is, against the repository's 10 GB cache limit. An entry is saved only when something new was
  fetched, so the count of entries stays small.
