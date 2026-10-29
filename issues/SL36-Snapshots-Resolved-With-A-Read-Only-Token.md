# SL36 — Snapshots Resolved With A Read-Only Token

**Status:** Review.
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

- **The contract read from outside:** the host's forge hands every build the secret `PORELO_PACKAGES_READ_TOKEN`, a
  token whose one scope is reading the organization's packages, and the variable `MAVEN_SNAPSHOTS_URL` names its
  registry. On GitHub the same secret holds a placeholder, since Sonatype needs none. Maven sends it with the user
  name `settings.xml` names today.
- **`settings.xml`:** the server `porelo-forge` takes its password from `PORELO_PACKAGES_READ_TOKEN` in place of
  `PORELO_PUBLISHER_TOKEN`; its comment says it only reads.
- **The workflows:** every step that runs Maven (`build.yml`'s GraalVM classpath, effective pom and `verify`,
  `shared-rules.yml`'s classpath and documents tests, `workflows.yml`'s classpath) gets `PORELO_PACKAGES_READ_TOKEN`, and
  `PORELO_PUBLISHER_TOKEN` is named in none of them.
- **A missing snapshot repository fails:** the step after the checkout of each job that runs Maven stops with an
  error naming `vars.MAVEN_SNAPSHOTS_URL` when it is empty and `github.server_url` is not `https://github.com`; on
  GitHub the guard lets an empty value pass, as today.
- **`doc/decisions.md`**, *The workflows run unchanged…*: the token that reads, and why the publisher's is never here.
- **Red first:** `WorkflowTokensTest`, tagged `documents`, reads the four workflows and `settings.xml` as lines and
  fails when any of them names `PORELO_PUBLISHER_TOKEN`, naming the file and line; it is red on today's workflows. A
  second assertion fails when a job that runs Maven has no step checking `MAVEN_SNAPSHOTS_URL` before it, and the
  guard's own script is run: it must stop on a forge without the variable and pass on GitHub and on a forge that
  names it. The guard carries no condition, a workflow's `*.yaml` is read too, and Maven is found however its path
  is spelt. The step reader moves out of `WorkflowCommitsTest` into `WorkflowSteps`, shared by both.
- **Declined in the reading against the skills**, each with its reason: an allowlist of every secret a workflow
  may name (the harm is the token that writes, and it is refused by name); checking the shape of
  `MAVEN_SNAPSHOTS_URL` (the guard stops a missing repository, not a wrong one); duplicate `env:` blocks,
  `@Nullable` and copied maps in the step reader (moved unchanged, outside this change); `printf` in place of `echo`
  in the guard (the server URL comes from the runner and reaches the script through its environment); the
  `central-sonatype` server in `settings.xml` (the credential for Maven Central, not the forge's, outside this
  change).

## Acceptance

- No workflow names `PORELO_PUBLISHER_TOKEN`, and the test says so; it was seen to fail on the workflows before.
- On GitHub the Build, the *Shared rules check* and the *Workflow check* run green with the placeholder secret and
  today's `MAVEN_SNAPSHOTS_URL`.
- On the host's forge the Build resolves its snapshots from the forge's registry with the read-only token, and with
  `MAVEN_SNAPSHOTS_URL` removed it fails in the step after its checkout, naming the variable.

## To be checked

- Whether the forge checks the user name Maven sends against the token's owner; if it does, `settings.xml` names
  the owner. Measured with the move to the host's forge.
