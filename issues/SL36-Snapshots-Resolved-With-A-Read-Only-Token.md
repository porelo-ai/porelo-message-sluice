# SL36 — Snapshots Resolved With A Read-Only Token

**Status:** Analyzed.
**Size:** S - four workflows, `settings.xml`, one test that reads the workflows and one decision; no code of the
filter, and the token's name is the only thing read from outside.

**What must be true.** No step of this repository's workflows holds a token that can write to a forge's package
registry: snapshots are resolved with a token that can only read, and a Build on a forge other than github.com that
names no snapshot repository stops at once instead of falling back to Sonatype's.

## Why

Every Maven step on the host's forge holds the publisher's token today, `verify` on an unreviewed task branch
included, so a test in such a branch could read it and overwrite a snapshot in the forge's registry. This repository
deploys nothing with Maven - its packages go to Artifactory with their own token, and only from `main` or a tag
on GitHub - so it needs no writing token for Maven at all.

## Plan

Read in today's `build.yml`, `shared-rules.yml`, `workflows.yml`, `settings.xml` and `doc/decisions.md`.

- **The contract read from outside:** the host's runner hands every build a read-only package token under a name of
  its own, and the forge's variable `MAVEN_SNAPSHOTS_URL` names its registry. On GitHub the same secret holds a
  placeholder, since Sonatype needs none.
- **`settings.xml`:** the server `porelo-forge` takes its password from the read-only token's variable in place of
  `PORELO_PUBLISHER_TOKEN`; its comment says it only reads.
- **The workflows:** every step that runs Maven (`build.yml`'s GraalVM classpath, effective pom and `verify`,
  `shared-rules.yml`'s classpath and documents tests, `workflows.yml`'s classpath) gets the read-only token, and
  `PORELO_PUBLISHER_TOKEN` is named in none of them.
- **A missing snapshot repository fails:** the first step of each job that runs Maven stops with an error naming
  `vars.MAVEN_SNAPSHOTS_URL` when it is empty and `github.server_url` is not `https://github.com`; on GitHub an empty
  value keeps today's fallback to Sonatype's snapshots.
- **`doc/decisions.md`**, *The workflows run unchanged…*: the token that reads, and why the publisher's is never here.
- **Red first:** a test tagged `documents` reads the four workflows as lines and fails when any of them names
  `PORELO_PUBLISHER_TOKEN`, naming the workflow and line; it is red on today's workflows. A second assertion fails
  when a job that runs `./mvnw` has no step checking `MAVEN_SNAPSHOTS_URL` before it. Each is seen red, then green.

## Acceptance

- No workflow names `PORELO_PUBLISHER_TOKEN`, and the test says so; it was seen to fail on the workflows before.
- On GitHub the Build, the *Shared rules check* and the *Workflow check* run green with the placeholder secret and
  today's `MAVEN_SNAPSHOTS_URL`.
- On the host's forge the Build resolves its snapshots from the forge's registry with the read-only token, and with
  `MAVEN_SNAPSHOTS_URL` removed it fails in its first step, naming the variable.

## To be checked

- The read-only token's variable name and scope, as the host's forge gives it to builds; until then the plan says
  "the read-only token".
- That the organization's secrets on GitHub hold the placeholder under that name for this repository.
