# SL56 — The Issue Heads Are Checked Every Day

```json
{
  "$schema": "issue-1.json",
  "id": "SL56",
  "title": "The Issue Heads Are Checked Every Day",
  "state": "Backlog",
  "size": "XS",
  "priority": "Low",
  "rank": 10,
  "blockedBy": [],
  "covers": "The Shared rules check runs `porelo-release issues check issues` once a day, so an issue head that breaks the shared shape fails a run even when no push built it."
}
```

**Why this size.** one step in `shared-rules.yml`, from a released `porelo-release`.

**What must be true.** Every issue head in `issues/` is checked against the shared issue schema by the daily *Shared
rules check*, so a head written wrong in an issues-only push, which the Build skips, is found within a day.

## Plan

- `.github/workflows/shared-rules.yml`: a step after *The documents cite no issue ...* runs
  `org.fuin.porelo.release.Main issues check issues` from the `-Pci-tools` classpath, on the schedule, on a pull
  request and by hand.
- The `ci-tools` profile pins a released `porelo-release` that has `issues check`.
- No test: the step is a workflow step; its proof is the run.

## Acceptance

- The step runs locally verbatim against `issues/` and passes; with one head's `state` set to a value outside the
  schema it fails, naming the file, and the head is put back.
- The next scheduled *Shared rules check* on GitHub shows the step green.

## To be checked

- Which released `porelo-release` version first carries `issues check`, and whether it is already the one the
  `ci-tools` profile pins.
