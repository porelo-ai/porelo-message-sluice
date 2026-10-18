# SL28 — Modes that cannot go together are refused

**Status:** Accepted.

**What must be true.** A command line that asks for two modes the filter cannot run together is
refused, with exit 2 and a line that names both, instead of running one of them and dropping the
other without a word.

## Why

Found when the code was read against the skills in 2026-10. `cli/Main` refuses `--file` without `--dry-run` and
`--dir` without `--calibrate`, but takes these and runs only the first mode it checks:

- `--agent-card` with `--calibrate`, `--dry-run` or `--watch`;
- `--calibrate` with `--file`;
- `--dry-run` with `--watch`.

A person who adds `--watch` to a dry run thinks it is watching, and it is not.

## To be checked

- **Whether Porelo or a transport calls the filter with such a pair today.** Refusing one changes
  which command lines are accepted, so it was not built with that reading's fixes.
- Whether `--agent-card` should stay a mode that ignores the rest, as `--help` does elsewhere.
