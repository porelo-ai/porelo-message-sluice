# SL33 — The Workflows Run On The Local Forge As On GitHub

**Status:** Implementing; Accepted by the operator's decision in porelo-project
[PJ60](https://github.com/porelo-ai/porelo-project/blob/main/issues/README.md#pj60) for sprint 4. Green on the
local Forgejo only after porelo-host [HO08](https://github.com/porelo-ai/porelo-host/blob/main/issues/README.md#ho08)
and porelo-parent's and porelo-buildtools' snapshots deployed to the forge's registry.
**Size:** S - four workflows, `settings.xml` and the documents that describe them; no Java.

**What must be true.** The four workflows run unchanged on the local Forgejo and on GitHub; what differs is only
the values of variables and secrets present on both forges, never a branch in a workflow.

## Plan

- **The JDK:** no change here. The act image the local Forgejo runs `ubuntu-24.04` and `ubuntu-26.04` on carries
  no Java, and `actions/setup-java` is refused by `check-actions`. So, by the operator's decision, the forge's
  runner mounts a pinned Temurin 25 as `JAVA_HOME_25_X64`, as GitHub's image provides one (porelo-host
  [HO08](https://github.com/porelo-ai/porelo-host/blob/main/issues/README.md#ho08)).
- **The tools:** a step in `build.yml` installs whatever of `gcc`, `zlib1g-dev`, `rpm2cpio`, `cpio` and `dpkg` is
  missing (on the act image: `rpm2cpio` and `cpio`; the .rpm is written by a Maven plugin, without `rpmbuild`); `workflows.yml` installs `python3-yaml` where it is missing. As root on the forge's image,
  with `sudo` on GitHub's runner.
- **Publishing:** the Publish and indexing steps also need `vars.PORELO_PUBLISH == 'true'`, and *Delete old runs*
  runs only then. The Build refuses a value other than `true` or `false`.
- **Snapshots:** `settings.xml` keeps Sonatype where `MAVEN_SNAPSHOTS_URL` is unset (a developer, a VM, a Porelo
  task). Where it is set, it reads that repository instead, under the id `porelo-forge` with user
  `porelo-publisher` and password `PORELO_PUBLISHER_TOKEN`, the id porelo-parent's `forge-release` uses. The
  workflows set the variable for the job and the token only on the steps that run Maven.
- **Red first:** today's `build.yml` on the local Forgejo, which stops at `$JAVA_HOME_25_X64`.

## Acceptance

- `build.yml`, `shared-rules.yml` and `workflows.yml` run green on GitHub and on the local Forgejo on the same
  commit; on the forge, Publish is skipped and the snapshots come from the forge's registry.
- Seen to fail first: today's `build.yml` on the local Forgejo.
- A `PORELO_PUBLISH` other than `true` or `false` stops the Build, naming the value.

## To be checked

- The run on the rented forge host, once it exists: its address, its CA, this repository's own
  agent token there, and porelo-parent's and porelo-buildtools' snapshots deployed in its registry.
  Then `main` is pushed to the forge and its run read: Publish skipped by `PORELO_PUBLISH=false`, and
  the five permission tests skipped where the runner is root.
- Whether GitHub hands `vars` to a Dependabot pull request's *Workflow check*: without them,
  `MAVEN_SNAPSHOTS_URL` is empty and Maven reads from an empty URL.
