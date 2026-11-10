# SL49 — A Credential's Context Is Read On Both Sides

```json
{
  "$schema": "issue-1.json",
  "id": "SL49",
  "title": "A Credential's Context Is Read On Both Sides",
  "state": "Backlog",
  "size": "XS",
  "priority": "Medium",
  "rank": 2,
  "blockedBy": [],
  "covers": "A credential whose context word follows it is refused under a test; PIT shows the window after the match held by none."
}
```

**Why this size.** one test of `CredentialDetector`.

**What must be true.** A credential rule that needs a context word finds the word after the match as well as before
it, so a credential followed by its context word is refused.

## Plan

PIT with the `pit` profile left `CredentialDetector.nearContext` alive with the end of its window moved before the
match: no test puts the context word only after a credential, so such a credential would pass.

- Red first: a credential of a rule that needs context, its context word only after it and more than the window's
  width from the text's start, refused.

## Acceptance

- The test fails against the mutant PIT left, seen red, and passes against the code; PIT on `CredentialDetector` leaves
  `nearContext` killed.
