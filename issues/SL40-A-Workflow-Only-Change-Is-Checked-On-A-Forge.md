# SL40 — A Workflow-Only Change Is Checked On A Forge

**Status:** Analyzed.
**Size:** S - one workflow and its measurement on a host's forge, no contract; larger only if the cause is Forgejo's
and needs a setting of the host.

**What must be true.** A pull request that changes only `.github/` is checked on a host's forge as it is on GitHub,
by the light *Workflow check*, and no job of this repository's workflows is silently ignored there; on GitHub every job
keeps a token that can only read.

## Plan

Read in today's four workflows. On a host's forge, on a pull request that changed only `doc/` and `issues/`, Forgejo
warned that a job `check` has a `permissions` field, which it does not support, and that the job is therefore ignored.
The *Shared rules check*'s job `check` ran green on the same pull request, and the Build's job `build`, started by
hand, ran green too; both workflows carry the same workflow-level `permissions: contents: read`. The other job named
`check` is `workflows.yml`'s, the light check of a change to `.github/` alone, which the Build leaves to it. If that is
the ignored one, a workflow-only change on a forge is checked by nothing.

- **Measure first, on a host's forge:** a pull request that changes only a comment in `.github/workflows/`. Read
  whether *Workflow check* runs, and the warning's workflow file on the run page. The test that goes red first is this
  pull request, with no run of `workflows.yml`.
- **The difference to find:** `workflows.yml` has `pull_request` with `branches` and `paths`, the others do not; its
  block is otherwise the same. Read which of these makes Forgejo ignore it, by one change at a time on the forge.
- **The fix keeps GitHub's guard:** the `permissions` block stays wherever it holds a token to reading, since on
  GitHub it is what does that. Only the form changes (workflow level or job level, or the trigger's shape), as far as
  the measurement says; `check-actions` and `WorkflowTokensTest` still pass.
- **If no form runs on Forgejo with the block:** that is the forge's limit; this issue then says so in
  `doc/decisions.md`, under *The workflows run unchanged on GitHub and on the local Forgejo*, and the gap goes to the
  operator as an open question.

## Acceptance

- On a host's forge, a pull request that changes only `.github/` runs *Workflow check* to its end, and Forgejo warns of
  no ignored job in any of the four workflows.
- On GitHub, the same pull request runs *Workflow check* with a token that can only read.
- Seen to fail: the pull request on the forge before the fix, with no run of `workflows.yml` or with the warning.

## To be checked

- Which workflow the warning named: what was relayed from the run page named only the job.
- Whether `delete-runs.yml`'s job-level `permissions` draws the same warning; it is skipped on a forge by its `if:`.
