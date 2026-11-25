# Review fixtures

Review profile 2 documents, each the one data part of a review comment; `ReviewFixturesTest` wraps
each into a comment to `review` and runs it through a mailbox.

- **`forge-page`, `submitted-comment`, `submitted-reply`** are Porelo's own passing fixtures from the
  tests jar of `porelo-build-api`, copied unchanged. A page passes only with `--inbound`, a submission
  only without it; each is refused in the other direction.
- **`refused/`** holds this repository's adversarial documents, each one of the passing ones with one
  fault. The name gives the direction (`out-` as a task sends it, `in-` with `--inbound`) and then the
  rule that refuses it, which is the only envelope rule it trips; its source passes, so that one
  fault is what refuses it.
