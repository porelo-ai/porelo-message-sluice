# SL34 — A Dependabot Pull Request Gets The Variables

**Status:** Idea.

**What must be true.** A Dependabot pull request's *Workflow check* on GitHub receives the repository variables,
so `MAVEN_SNAPSHOTS_URL` names the snapshot repository and Maven does not read from an empty URL.

## Measured

An empty `MAVEN_SNAPSHOTS_URL` does not make Maven read from an empty URL. With Maven 3.9.15 and this repository's
`settings.xml`, `help:active-profiles` names the `standard` profile when the variable is empty, as when it is unset,
and `forge` when it names a repository. The *Workflow check*'s own Maven call with the variable empty, on an empty
local repository, succeeded and fetched the snapshots from Sonatype under `central.sonatype.snapshots`.

The same step on GitHub with a real Dependabot pull request was not run; the variable's being empty there is what
this issue states, not something measured here.

**Proposed:** close this issue. On github.com an empty variable means what the variable would name there, Sonatype,
and on any other forge the job stops before Maven runs.
