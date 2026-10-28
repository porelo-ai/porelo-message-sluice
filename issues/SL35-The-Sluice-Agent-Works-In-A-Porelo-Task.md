# SL35 — The Sluice's Agent Works In A Porelo Task

**Status:** Analyzed.
**Size:** S - nothing to build in this repository's code; the work is the move's steps, one document corrected in the task
and this issue worked to Review from inside it, about 3 hours with the forge's build.

**What must be true.** porelo-message-sluice's own agent works in a Porelo task of the project `porelo` on the
move's host, the first to move, as porelo-project
[PJ59](https://github.com/porelo-ai/porelo-project/blob/main/issues/README.md#pj59) lays out for sprint 7. The task
follows this repository through the gate and pushes to the host's Forgejo, whose build is read through the `forgejo`
reader. The agent is reached in the project's room under this repository's name. This issue is taken from start to
Review inside the task, with the forge's build green.

## Plan

Read in today's `porelo.yml`, `settings.xml`, the four workflows, `doc/decisions.md`, porelo-host's
`roles/forge` and `doc/local-forge.md`, and [PJ59](https://github.com/porelo-ai/porelo-project/blob/main/issues/README.md#pj59)'s procedure.

- **What moves:** `porelo-message-sluice` alone. The agent in the task is Claude Code (porelo-claude-code's adapter,
  the operator's subscription, its isolation proven by
  [CC24](https://github.com/porelo-ai/porelo-claude-code/blob/main/issues/README.md#cc24)); it declares its resting
  and waiting screens, so a message in the room wakes it.
- **The image needs nothing new:** `porelo.yml` names Ubuntu 24.04 by digest, the pinned GraalVM, shfmt and the
  package tools, the pins the Build takes. [SL29](README.md#sl29) ran the whole local check, native images and both
  packages, in a task built from it.
- **Forgejo's build needs nothing new in the workflows:** the host's `up` sets `PORELO_PUBLISH=false`,
  `MAVEN_SNAPSHOTS_URL` to the forge's registry and `PORELO_PUBLISHER_TOKEN`, and the runner offers both labels the
  workflows name (`ubuntu-24.04`, `ubuntu-26.04`), as [SL33](README.md#sl33) measured on the sprint 4 forge.
- **Two stages.** First a dry run on this laptop, *online* with GitHub as upstream, driven from the interface: the
  task, the room on this laptop's homeserver, the wake, the gate's push to GitHub and the `github` reader. The `github` reader opens no pull request, so
  there the person opens it; the gate opening it waits for the host's forge. It
  shows everything but the forge, at no rental. The project it follows must name this laptop's own homeserver
  (`matrix: {}`), never a host's room. Then the move proper on the host, which changes only the upstream and adds the
  forge; the steps below are that move. This issue reaches Review only there.
- **The reading a hand-over needs:** neither Codex CLI nor Antigravity CLI is in the task. Until a reviewer task
  ([B172](https://github.com/porelo-ai/porelo/blob/main/issues/base/README.md#b172)) exists, an agent outside the task reads the pull request's diff and posts its findings in the
  room; the task's agent answers each there, as a local agent does with a sub-agent's reading today.
- **The steps, in [PJ59](https://github.com/porelo-ai/porelo-project/blob/main/issues/README.md#pj59)'s order:**
  1. *Before:* the forge's registry holds porelo-parent `0.2.1-SNAPSHOT` and porelo-buildtools' `0.5.1-SNAPSHOT`
     modules (`porelo-release`, `porelo-machines`), published there by their agents, since the build's `forge`
     profile reads snapshots only from that registry. This issue's text is the first commit the task makes.
  2. *The switch:* the coordinator's `project.yml` commit gives this repository the host's forge as upstream.
  3. *The start:* `porelo task start --repository porelo-message-sluice` on the host, attended, the agent named
     `Agent Sluice` from the nickname ([B298](https://github.com/porelo-ai/porelo/blob/main/issues/base/README.md#b298)).
  4. *Inside the task:* the agent reads its context, fetches the seven skills (below), sets this issue to
     Implementing, makes the correction below, runs the documents tests and the full suite in the task, and
     pushes. The gate opens the pull request
     ([B297](https://github.com/porelo-ai/porelo/blob/main/issues/base/README.md#b297)); the agent reads the Build and
     the *Shared rules check* through the `forgejo` reader, both green, and sets Review.
  5. *The check:* `move-check.sh porelo-message-sluice`, red before the start, green after; then this machine's
     session and the `sluice` VM accounts are no longer used.
- **The one correction made inside the task:** `doc/decisions.md`, *The workflows run unchanged…*, still says a
  forge pull request fails the commit-message step; since `a40ea15` the step fetches nothing and counts a pull
  request from the same repository as inside. Rewritten once the gate's pull request shows the step green on the
  forge.
- **The way back:** none beyond the move's own step 5, which holds for every agent.
- **No test goes red first:** nothing that can do harm changes in the code. The move's red-first check is
  `move-check.sh` against this repository before the start.

## Acceptance

- `move-check.sh porelo-message-sluice` fails on every line before the start and passes after: the task runs, its
  last push reached the forge as `porelo/<task>`, the `forgejo` reader read its Build, the gate opened the pull
  request, and the task is in the room's `agent-card.json`.
- This issue's file says Review in the commit the operator merges, and that commit's Build and *Shared rules check* on
  the forge are green.
- A message naming `Agent Sluice` in the room wakes the task's agent, and its answer appears in the room.
- Seen to fail: the check against the repository before its move.

## To be checked

- That the host's inventory names this repository for its forge (`forge_repositories`): the forge role migrates a
  repository, protects its `main` and makes the gate's identity only for those named, and skips all of it otherwise.
- That the forge's registry holds porelo-parent's and porelo-buildtools' snapshots no older than GitHub's on the day
  of a host move, and who republishes them there while their repositories have not moved.
- What the task's agent is given in its context in place of `.AGENTS.md`, which is not committed and so not in the
  task: its name, the room in place of the channel, the operator's language and time zone. That is [PJ59](https://github.com/porelo-ai/porelo-project/blob/main/issues/README.md#pj59)'s rules
  table; what is missing is said to the coordinator before the start.
- That the task's egress reaches what the build and the agent fetch: Maven Central, Sonatype's snapshots (in the dry run),
  GitHub's release downloads (GraalVM, shfmt), and `fuinorg.jfrog.io` for the skills, which [SL29](README.md#sl29) did not fetch.
- Where Claude Code in the task reads the skills (`~/.claude/skills/<slug>/` in the task's home), and that they are
  checked against their digests there.
- Which of the five permission tests that skip as root still run in the task, named in the hand-over.
