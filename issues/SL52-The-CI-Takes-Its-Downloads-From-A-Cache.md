# SL52 — The CI Takes Its Downloads From A Cache

```json
{
  "$schema": "issue-1.json",
  "id": "SL52",
  "title": "The CI Takes Its Downloads From A Cache",
  "state": "Analyzed",
  "size": "S",
  "priority": "Medium",
  "rank": 3,
  "blockedBy": [],
  "covers": "The pinned GraalVM and the pinned shfmt come from the Actions cache, keyed by their digests, so a server that answers 504 for a download it already served fails no Build; only the newest entry of each is kept.",
  "openQuestions": 0
}
```

**Why this size.** Three workflows of this repository and their check by a run on a forge, no contract and no code that
ships; larger only if the cache entries must be deleted by the workflow itself.

**What must be true.** A Build, a *Shared rules check* or a *Workflow check* whose outside download was fetched once
before takes it from the cache, checked against its digest, and does not fail when its server is down for a while.
The cache never fills up: each entry is keyed by the digest or version it holds, and an older one is removed.

## Plan

Read in today's four workflows. What each job fetches from outside on every run:

- **The pinned GraalVM**, several hundred megabytes, in `build.yml` and `shared-rules.yml` (step *The pinned
  GraalVM*): `porelo-machines jdk --github`, without `--cache`, so every run downloads it. `porelo-machines` already
  offers `jdk --print-sha256` (the pinned digest, fetching nothing) and `jdk --cache <dir>` (keeps the checked
  archive as `<sha256>.tar.gz` and checks it again after a hit).
- **The pinned shfmt**, in `build.yml` and `shared-rules.yml` (step *The pinned shfmt*): `curl` from GitHub's releases,
  checked by its digest, every run.
- **The distribution's packages**, `apt-get install` of `gcc`, `rpm`, `rpm2cpio`, `cpio`, `dpkg`, `zlib1g-dev` in
  `build.yml` and `python3-yaml` in `workflows.yml`, only where the runner's image lacks them.

Already cached: the Maven repository (`build.yml`, `shared-rules.yml`) and the wrapper's Maven (all three). The
publishing step's `curl` calls to the package repository are the target of the publish, not a download to cache.
`delete-runs.yml` fetches nothing.

Steps:

1. The GraalVM: before *The pinned GraalVM*, take the digest from `jdk --print-sha256` on the ci-tools classpath,
   `actions/cache/restore` with the key `pinned-graalvm-<sha256>` on a cache directory, call `jdk --github --cache
   <dir>`, and `actions/cache/save` on a miss - the shape `porelo-buildtools`' `pinned-jdk` action has.
2. The shfmt: the same with the key `pinned-shfmt-<sha256>`, the digest already written in the step.
3. The distribution's packages: not cached where the runner already has them; on a forge whose image lacks them,
   `apt-get` reads the distribution's signed mirrors, which a cache of `.deb` files would not make safer. Kept as it
   is, unless *To be checked* finds the GitHub runner installing them on every run.
4. Older entries: a key that names a digest is never written twice, and a new pin writes a new key. The older entry
   is removed by GitHub's own eviction or, if that is not enough, by the workflow after it saved the new one (see *To
   be checked*). The Maven repository's key changes with every `pom.xml` change and is restored from the newest
   `maven-` entry, so it is held to the same rule.

The test: a workflow change only, with no code that ships. `WorkflowCommitsTest` reads `build.yml` and
`shared-rules.yml` and runs their commit-message steps; it runs unchanged, and a run on the local forge shows the
cache hit on the second run.

## Acceptance

- A second run of the Build and of the *Shared rules check* on the same pin downloads neither the GraalVM nor shfmt:
  the log shows the cache hit and the digest check after it.
- A cached archive whose digest does not match fails the step with both digests named.
- After a pin moves, the cache holds the new entry, and the old one is gone or marked for removal by the rule chosen.

## To be checked

- Whether the GitHub runner `ubuntu-26.04` already holds `gcc`, `rpm`, `rpm2cpio`, `cpio`, `zlib1g-dev` and
  `python3-yaml`, read from a run's log: if not, `apt-get` runs on every Build.
- Whether GitHub's repository settings can remove a cache entry after a set time, beyond its default eviction of
  entries not used for seven days; if not, whether deleting the older entry needs the job's token with `actions:
  write`, which the workflows do not grant today (`contents: read`).
- Whether the local forge's Actions offer `actions/cache` with the same behaviour, so the steps work on both forges.
