# SL35 — The Sluice's Agent Works In A Porelo Task

**Status:** Implementing; waits until a task on its host fetches the snapshots it builds against from the host's
forge, which none can yet.
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
- **Snapshots:** the task sets no `MAVEN_SNAPSHOTS_URL`, so `settings.xml`'s `standard` profile reads them from
  Sonatype there; the forge's Build sets it and reads them from the forge's registry under `porelo-forge`.
- **Forgejo's build needs nothing new in the workflows:** the host's `up` sets `PORELO_PUBLISH=false`,
  `MAVEN_SNAPSHOTS_URL` to the forge's registry and a token that only reads it, and the runner offers both labels the
  workflows name (`ubuntu-24.04`, `ubuntu-26.04`), as [SL33](README.md#sl33) measured on a host's
  local Forgejo, recorded in `96509d5`.
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
     the `github` reader). The forge's registry holds the snapshots the forge's Build resolves - the parent pom
     `org.fuin.porelo:porelo-parent` and the release tools (`porelo-release`, `porelo-machines`) - no older than
     Sonatype's on the day. The project's registration on the host brings this repository onto its forge, with `main`
     protected and the gate's identity made.
  2. *The switch:* the coordinator's `project.yml` commit gives the repository, nickname `sluice`, the host's forge as
     upstream.
  3. *The start:* `porelo task start --repository porelo-message-sluice --agent claude` on the host, attended, its
     terminal opened by the operator.
  4. *Inside the task:* the agent reads its context, fetches the seven skills into `~/.claude/skills/<slug>/`, each
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

- On the host, before the start: the forge's registry holds the snapshots named in step 1, and the forge holds this
  repository with `main` protected.
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
