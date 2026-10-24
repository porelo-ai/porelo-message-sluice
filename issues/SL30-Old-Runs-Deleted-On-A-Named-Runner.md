# SL30 — Old runs deleted on a named runner

**Status:** Review.
**Size:** XS - one line in each of two workflows, `.github/` only, no Build and no changelog entry.

**What must be true.** *Delete old runs* runs on `ubuntu-26.04`, the current standard, instead of `ubuntu-latest`,
so a change of what GitHub's `ubuntu-latest` means cannot change what it runs on without a commit here.

## Plan

- `delete-runs.yml` from `ubuntu-latest`, and `shared-rules.yml` from `ubuntu-24.04`, to `ubuntu-26.04`, the
  label GitHub's runner list offers. Both build nothing that ships. `build.yml` stays on `ubuntu-24.04`, since it
  builds the native filter whose glibc floor `LinkageIT` holds.
- Nothing guards it with a test: a runner label is checked by the run it starts.

## Acceptance

- The first *Shared rules check* on `ubuntu-26.04` after the push is green, read in its log, its runner image
  named there.
- *Delete old runs*, started by hand without `delete`, lists what would go, on `ubuntu-26.04`.
