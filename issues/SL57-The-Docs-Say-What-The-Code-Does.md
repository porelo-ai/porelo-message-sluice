# SL57 — The Docs Say What The Code Does

```json
{
  "$schema": "issue-1.json",
  "id": "SL57",
  "title": "The Docs Say What The Code Does",
  "state": "Review",
  "size": "S",
  "priority": "Highest",
  "rank": 1,
  "blockedBy": [],
  "covers": "Every sentence of the user-facing documentation that the code on `main` does not bear out is found by a reading page by page, checked in the code, and fixed in the document; a wrong code becomes a bug issue of its own."
}
```

**Why this size.** documents only, about 500 lines in one repository, no behavior change.

**What must be true.** A user who follows `README.md`, `doc/`, `build.md` or the changelog's unreleased section
finds every command, flag, path, default, limit, message and behavior as the program on `main` has it.

## Plan

- A read-only reading of `README.md`, `doc/index.md`, `doc/filter.md`, `doc/detectors.md`, the `[Unreleased]`
  section of `CHANGELOG.md`, `build.md`, `CONTRIBUTING.md`, `SECURITY.md` and the modules' `README.md` against
  `filter/`, the packaging, `install-local.sh`, `buildtools/` and the workflows; `doc/decisions.md` and `issues/` are
  not read, as they hold reasons, not instructions.
- Each finding is checked in the code before anything changes. Where the code is right, the document is fixed;
  where the document is right, the code's fault becomes a bug issue of its own and the document stays.
- No test goes red first: nothing that runs changes.

## Acceptance

- Every confirmed finding is fixed in its document or filed as a bug; the count of claims checked, confirmed, fixed
  and filed is in the hand-over.
- The tests tagged `documents`, `check-commits`, `check-citations` and `check-doc-site` pass on the commit.
