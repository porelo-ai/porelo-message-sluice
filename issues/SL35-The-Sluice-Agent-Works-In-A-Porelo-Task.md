# SL35 — The Sluice's Agent Works In A Porelo Task

**Status:** Review.
**Size:** S - nothing to build in this repository's code; the work is the trial move's steps, one document corrected in
the task and this issue worked to Review from inside it, about 3 hours: 1½ of the agent's work, the rest the forge's
Build, the reading from outside and the way back.

**What must be true.** porelo-message-sluice's own agent works in a Porelo task of the project `porelo` on the
move's host, the first to move, as the project's procedure for an agent's move lays out. The task
follows this repository through the gate and pushes to the host's Forgejo, whose build is read through the `forgejo`
reader. The agent is reached in the project's room under this repository's name. This issue is taken from start to
Review inside the task, with the forge's build green.

## Plan

Read in today's `porelo.yml`, `settings.xml`, the four workflows, `doc/decisions.md`, the host's forge setup and its
local registry settings, and the project's procedure for an agent's move.

- **What moves:** `porelo-message-sluice` alone, as a trial: it works in the task on the move's host until its check
  is green, then takes the way back and is worked on this machine again until the real move of every repository.
  The agent in the task is Claude Code with the operator's subscription from the host's vault; it declares its
  resting and waiting screens, so a message in the room wakes it.
- **The image needs nothing new:** `porelo.yml` names Ubuntu 24.04 by digest, the pinned GraalVM, shfmt and the
  package tools, the pins the Build takes. [SL29](README.md#sl29) ran the whole local check, native images and both
  packages, in a task built from it.
- **Snapshots come from the central source; the host's registry holds only what was built there and has not yet gone
  through the central forge.** The task sets no `MAVEN_SNAPSHOTS_URL`, so `settings.xml`'s `central` profile alone reads them
  from Sonatype there, which this trial is content with: it changes no snapshot another repository builds against.
- **`settings.xml` reads both on a forge**, the central source and the registry under `porelo-forge-snapshots`, the
  newest winning, with `.mvn/rrf/` keeping every group but `org.fuin.porelo` from the central source. Measured with
  this repository's build and an empty local repository: a registry answering 404 to everything builds green from the
  central source; a private group's name is asked of the registry and not of a stand-in for the central source, and
  with no filter file it is asked of both. On a real forge it is not yet measured. `check-snapshot-filter` holds the
  shape.
- **Forgejo's build needs nothing new in the workflows:** the host's `up` sets `PORELO_PUBLISH=false` and
  `MAVEN_SNAPSHOTS_URL` to the forge's registry, and the runner offers both labels the workflows name (`ubuntu-24.04`,
  `ubuntu-26.04`), as [SL33](README.md#sl33) measured on a host's local Forgejo, recorded in `96509d5`. The host's
  project setup gives each project the token that only reads the registry, which the workflows take as
  `PORELO_PACKAGES_READ_TOKEN`, and a Build without it stops at once.
- **What the task's context gives the agent in place of `.AGENTS.md`**, which is not committed: its name
  `Agent Sluice`, its attribution `Assisted-by: Claude Code, <model>` and no other trailer (check-commits refuses a
  `Co-Authored-By:`), that its upstream is the host's forge and the central forge only through the gathered push,
  the room in place of the channel, and the operator's language and time zone. What is missing is said to the
  coordinator before the start.
- **The reading a hand-over needs:** neither Codex CLI nor Antigravity CLI is in the task. The coordinator reads the pull
  request's diff from outside, with an agy Gemini reading against the seven skills, and posts the findings in the
  room; the task's agent answers each there, and the operator merges after them.
- **A fix after the reading amends the task's commit**, pushed with `--force` to the task's own unmerged branch,
  which the gate passes on; the work stays one commit, and `main` is never pushed with force.
- **A revoked sign-in** is renewed with `porelo vault login claude --force` and a restart of the task, which keeps the
  token it read at its start; a `429` after that is the account's limit.
- **The steps, in the move procedure's order:**
  1. *Before:* the dry run on this laptop passed (done: the task, the room, the wake, the gate's push to GitHub and
     the `github` reader). The forge's Build resolves the parent pom `org.fuin.porelo:porelo-parent` and the release
     tools (`porelo-release`, `porelo-machines`) from the central source, so neither is built on the forge unless it is
     changed there. The project's registration on the host brings this repository onto its forge, with `main`
     protected and the gate's identity made.
  2. *The switch:* the coordinator's `project.yml` commit gives the repository, nickname `sluice`, the host's forge as
     upstream.
  3. *The start:* `porelo task start --repository porelo-message-sluice --agent claude` on the host, attended, its
     terminal opened by the operator.
  4. *Inside the task:* the agent reads its context, fetches the seven skills into Claude Code's skills folder, each under its slug,
     checked against its digest, makes the correction below (the issue is set Implementing on `main` before the
     switch, so the forge receives that state), runs the documents
     tests and the full suite in the task, and pushes. The gate opens the pull request; the agent reads the Build and
     the *Shared rules check* through the `forgejo` reader, both green, answers the reading, and sets Review.
  5. *The check:* the move's check for this repository, red before the start, green after.
  6. *The way back:* what the forge holds of the task's work is fetched into the checkout here, the coordinator
     switches the upstream back, and the session here resumes with its watcher.
- **The one correction made inside the task:** `doc/decisions.md`, *The workflows run unchanged…*, still says a
  forge pull request fails the commit-message step; since `a40ea15` the step fetches nothing and counts a pull
  request from the same repository on a forge other than GitHub as inside (read again in today's
  `shared-rules.yml`). Rewritten once the gate's pull request shows the step green on the forge.
- **No test goes red first:** nothing that can do harm changes in the code. The move's red-first check is the move's
  check against this repository before the start.

## Acceptance

- The move's check for this repository fails on every line before the start and passes after: the task runs, its
  last push reached the forge as `porelo/<task>`, the `forgejo` reader read its Build, the gate opened the pull
  request, and the task is in the room's `agent-card.json`.
- This issue's file says Review in the commit the operator merges, and that commit's Build and *Shared rules check* on
  the forge are green.
- A message naming `Agent Sluice` in the room wakes the task's agent, and its answer appears in the room.
- After the way back, this checkout holds the task's commit and the session here works on it.
- Seen to fail: the check against the repository before its move.

## To be checked

- On the host, before the start: the forge holds this repository with `main` protected, and its Build resolves what
  step 1 names with the registry empty.
- That the task's egress reaches what the build and the agent fetch: Maven Central, Sonatype's snapshots, GitHub's
  release downloads (GraalVM, shfmt), and `fuinorg.jfrog.io` for the skills, which [SL29](README.md#sl29) did not
  fetch.
- That the task's context gives what the plan names in place of `.AGENTS.md`, the attribution above all: the dry run's
  Claude wrote a `Co-Authored-By:` from its own default.
- Which of the five tests that skip as root run in the task, named in the hand-over:
  `aFileThatCannotBeReadStopsTheScanRatherThanLettingItsDuplicateThrough`,
  `aTechnicalErrorLeavesTheMessageWhereItWasAndSaysSo`, `aTechnicalErrorFilesNothingAndLeavesTheMessageWhereItWas`,
  `aDirectoryHoldingRefusedOriginalsMustBeOwnedByTheUserTheFilterRunsAs` and
  `aMessageThatCannotBeFiledIsAnsweredOnceHoweverOftenItIsTried`.

## Findings from the trial

- **The Shared rules check is green on the forge.** Pull request #1 (`porelo/sluice` to `main`, commit `3c5319d`)
  ran it as `pull_request` in 2m48s, its step "The pull request's commit messages" included; `doc/decisions.md`
  now says so.
- **The Build does not start on a documents-only change**, by `build.yml`'s paths filter. It was started by hand
  (`workflow_dispatch`) on `porelo/sluice`: run #3, green in 4m08s on the runner `porelo-host`.
- **The task's egress reaches `fuinorg.jfrog.io`** (second pass: it resolves and answers 200; the first pass's
  allowed set did not include it). The seven skills AGENTS.md names, `graal` 2026.910.3 and `java-code-review`,
  `test-quality`, `security-audit`, `concurrency-review`, `clean-code` and `solid-principles` 2026.906.3, were
  fetched into the agent's skills folder, each under its slug and checked against the sha256 the store reports before it was
  unpacked; none failed.
- **Forgejo warned that a job `check` with a `permissions` field is not supported and is ignored.** Not
  `shared-rules.yml`'s, which ran; `workflows.yml` has the other job `check`, and `build.yml`, with the same
  workflow-level block, ran too. Followed in [SL40](README.md#sl40).
- **All five tests that skip as root ran and passed** in the task (user 1001), in both passes: the second pass's
  documents tests (27) and full `verify` (279, none skipped) exited 0.
- **No `forgejo` reader is available inside the task**, in both passes: no reader and no `porelo` program is on the
  path, so the operator relayed the forge's results through the coordinator. The helper that hands a task its builds
  (`porelo task watch-builds`) starts only when the project declares `builds:` and is `security_class` online;
  `porelo` is guarded with no `builds:` (the project's move to online, building on the host's own Forgejo, is not
  done yet), so the task is told nothing, and `porelo build status` on the host refuses for the same reason.
- **`git config user.name` says `Agent Sluice`**, as it should; the address is `agent@localhost`, and the commits are
  signed through the gate's signing service.
- **Nothing in the task sets the commit trailer.** There is no template, hook or git setting for it; the attribution
  reminder the harness gives the agent asks for a `Co-Authored-By:`, which the operator's instructions override with
  `Assisted-by: Claude Code, <model id>`. The trailer is written by hand, and the agent reads it before it pushes.
- **The second pass changes only this issue's findings, so the skills had no code to be read against.** All seven
  were read for their scope (Java review, tests, security, concurrency, native images, naming, class design) and
  none applies to prose in an issue file; nothing was found, and nothing was declined.
- **The second pass's Shared rules check failed once, then passed.** Run #1 failed with "names a requirement number":
  the commit message named the issue. The agent had run no commit check before its first push, so its own check did not
  stop it; after the reword it passed locally, and run #2 was green in 1m0s.
- **The second pass's Build is green.** `build.yml` #4 on `porelo/sluice`, started by hand (`workflow_dispatch`),
  ran 2m44s. The first pass's run #3 above stands as it is.
