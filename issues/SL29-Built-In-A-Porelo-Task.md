# SL29 — Built in a Porelo task

**Status:** Review.
**Size:** M - one image declaration and two lines of `.github/`, but its acceptance is a whole local check run
inside a real Porelo task, the native images and both packages among it.

**What must be true.** The repository names the tools its whole local check needs in `porelo.yml`, and that check
passes inside a Porelo task built from it, without the host's checkout, toolchain or Maven repository. This is
porelo-project [PJ39](https://github.com/porelo-ai/porelo-project/blob/main/issues/README.md#pj39)'s phases 2 and 5
for this repository.

## Plan

- **`porelo.yml`**, in porelo-message-matrix's shape: Ubuntu 24.04 and the GraalVM at porelo-machines' pins, the
  shfmt `build.yml` pins, and from Ubuntu `build-essential zlib1g-dev` for native-image, `rpm` for the `.rpm`,
  `rpm2cpio cpio` for the fixture step, `curl git unzip ca-certificates` for the wrapper and the tests that read the
  tracked files. `JAVA_HOME` and `GRAALVM_HOME` point at the GraalVM. Tools only, never egress or limits.
- **`build.yml`** ignores `porelo.yml` on push and pull request; **`CODEOWNERS`** gives it to the owner.
- **Until porelo [B192](https://github.com/porelo-ai/porelo/blob/main/issues/base/README.md#b192) reads it**, the run
  takes a disposable project outside every repository: a git repository on the VM whose `project.yml` copies the
  `image` block (the `env` as `ENV` lines in the snippet, which `project.yml` has no key for), `guarded`, the one
  repository `porelo-message-sluice` with a clone of the exact commit as its upstream, followed `--unverified`.
- **In the task, as the Build and the Shared rules check run them**, each exit code read: `-Pci-tools` resolved and
  `check-actions`; `./mvnw -B -s settings.xml -Pnative,dist verify` with a local snapshot counter; the packaged
  binaries against the fixtures, the step taken verbatim from `build.yml` with `RUNNER_TEMP` and `GITHUB_WORKSPACE`
  set; `check-shared`, `check-citations`, `check-doc-site`, `check-readmes`; `-Pdocuments test`.
- **The egress** is `maven`, `central.sonatype.com` for the snapshots and `raw.githubusercontent.com`, from which
  `check-citations` reads the other repositories' indexes. Agent `claude` refuses that host on purpose, and a refusal
  wins over the project's egress, so the task runs with an agent that does not, started `--attach shell`, which needs
  no credential.
- **Measured in such a task** on the Ubuntu VM, 6g and 3 CPUs beside another task build: the image builds in about
  3.5 minutes, the whole check runs in about 6, the native build alone in 2.5 to 5.

## Acceptance

- Every step above exits 0 inside the task, on the exact commit, and the result names every skipped test.
- What cannot run in a task is named with its reason: the GitHub actions (checkout, cache), `jdk --github` (it
  writes the runner's environment files; the image's GraalVM stands in for it) and the publishing steps (they hold
  the JFrog token, which no task does).
- The run removes its task, its image, its project and its checkout, and their absence is read.
