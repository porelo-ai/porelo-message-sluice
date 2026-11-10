# SL48 — The Remaining Classes Sorted

```json
{
  "$schema": "issue-1.json",
  "id": "SL48",
  "title": "The Remaining Classes Sorted",
  "state": "Backlog",
  "size": "S",
  "priority": "Medium",
  "rank": 9,
  "blockedBy": [],
  "covers": "The 120 mutants left in eleven classes not sorted first, sorted the same way, `SshSignature`, `DirectoryLock` and `MessageArchive` first."
}
```

**Why this size.** the same sorting for 120 mutants in eleven classes.

**What must be true.** The mutants PIT leaves in the classes not yet sorted are sorted like the others, real gaps and harmless ones, and the real gaps written as issues.

## Plan

PIT with the `pit` profile left 120 mutants alive or uncovered in classes that were not sorted first: `Calibration`
35, `Answers` 20, `Main` 16, `SshSignature` 13, `Settings` 9, `DirectoryLock` 8, `CheckConfig` 8, `MessageArchive` 7,
`A2aReader` 2, `DecisionConfig` 1 and `Unprocessable` 1.

- Each sorted by the categories of the first sorting: a real gap, or harmless as an edge, wording, equivalent,
  durability, a check that fails closed, or reached only through `MainTest`'s own JVM.
- No test goes red first: this issue sorts; the real gaps it finds go red in their own issues.
- `SshSignature`, `DirectoryLock` and `MessageArchive` first: they guard who sent a message, one filter at a time,
  and a message processed twice.

## Acceptance

- Every one of the 120 is sorted in this issue, each harmless one with its reason, each real gap in an issue.
