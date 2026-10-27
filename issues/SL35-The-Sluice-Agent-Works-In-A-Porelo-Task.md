# SL35 — The Sluice's Agent Works In A Porelo Task

**Status:** Idea.

**What must be true.** porelo-message-sluice's own agent works in a Porelo task of the project `porelo` on the
move's host, the first to move, as porelo-project
[PJ59](https://github.com/porelo-ai/porelo-project/blob/main/issues/README.md#pj59) lays out for sprint 7. The task
follows this repository through the gate and pushes to the host's Forgejo, whose build is read through the `forgejo`
reader. The agent is reached in the project's room under this repository's name. This issue is taken from start to
Review inside the task, with the forge's build green.

## What moves, and what to check

- **The repository:** `porelo-message-sluice` alone, the only one this agent keeps.
- **The task's image:** this repository's `porelo.yml`, Ubuntu 24.04 by digest with the pinned GraalVM, shfmt and the
  package tools, the same pins the Build takes. It ran this repository's whole local check in a task once already.
- **Forgejo's build:** `build.yml` runs unchanged on the forge and on GitHub, switched only by variables: snapshots
  read from the forge's registry through `MAVEN_SNAPSHOTS_URL`, and Publish and *Delete old runs* switched off by
  `PORELO_PUBLISH`. Publishing the packages stays with the central forge, since no task holds its credentials.
- **What the task does not have today**, read before the move: the sub-agents this agent builds and reads with (Codex
  CLI, Antigravity CLI) and their sign-ins, the skills from the shared skills repository, and the local VM, which
  today runs the workflow in a container before a handover. The forge's build replaces that VM run.
- **The name in the room:** `Agent Sluice`, from the nickname `Sluice` that porelo-project's `project.yml` gives
  this repository once porelo [B298](https://github.com/porelo-ai/porelo/blob/main/issues/base/README.md#b298) is
  published.
- **The way back:** none beyond the move's own step 5, which holds for every agent.
