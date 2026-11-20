# SL59 — The Maven Cache Follows What Was Resolved

```json
{
  "$schema": "issue-1.json",
  "id": "SL59",
  "title": "The Maven Cache Follows What Was Resolved",
  "state": "Review",
  "size": "XS",
  "priority": "High",
  "rank": 1,
  "blockedBy": [],
  "covers": "The Build and the Shared rules check restore the Maven repository under a key of their run's own and save it at the job's end only when the job fetched a jar or pom. Before, the key hashed only this repository's poms, and an exact hit was never saved over."
}
```

**Why this size.** It takes over the shared Porelo form of the Maven cache as a copy: one new composite action, the
cache step in two workflows, and the test that reads them. That is minutes of measured wall time; the form itself was
built and proven elsewhere.

**What must be true.** A Build whose poms did not change resolves nothing from Maven Central that an earlier Build
already fetched, so a refusal such as `429 Too Many Requests` cannot fail a Build that changed no dependency.

## Why

Both workflows cached the local Maven repository with `actions/cache` under the key
`maven-${{ hashFiles('**/pom.xml') }}`. The versions resolved come largely from `porelo-parent`, a snapshot, and the
BOMs it imports, and those move without any pom here changing. After an exact hit, `actions/cache` saves nothing, so
whatever they moved to was downloaded again on every run.

## Plan

- `build.yml` and `shared-rules.yml` take the shared form inline, since this repository has no `pinned-jdk` action:
  - an `actions/cache/restore` step `maven-cache`, keyed by the poms, the run id and the attempt, restoring by the
    pom key's prefix and then by `maven-<os>-`;
  - a marker touched right after it.
- `.github/actions/maven-cache-save` is new and copied unchanged from the shared form. It saves under the restore
  step's key only when a jar or pom changed status after the marker (`find -cnewer`, since Maven dates a downloaded
  file by the server's Last-Modified).
- Each workflow's one job ends with that action, with `if: always()`.
- `WorkflowMavenCacheTest` reads both workflows. It was seen red on the old cache steps and green on the new ones.
- `WorkflowSteps` reads a literal block value under `with:` (the restore keys), which its reading refused before.

## Acceptance

- `WorkflowMavenCacheTest` fails on the old cache steps, seen red, and passes on the new ones.
- The first Build after the push logs "fetched after the restore, among others: ..." and saves. The next Build with no
  pom change logs "nothing fetched after the restore: nothing saved", and its Maven output shows no artifact
  downloaded from Central.
