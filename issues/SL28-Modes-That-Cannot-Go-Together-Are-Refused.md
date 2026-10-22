# SL28 — Modes that cannot go together are refused

**Status:** Implementing.
**Size:** S - one class, `cli/Main`, its test, a line of `doc/filter.md` and a changelog entry; no contract, since
no caller passes such a pair.

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

## Plan

- **`cli/Main.runChecked`**, read on `45008f8`: the loop sets `agentCard`, `calibrate`, `dryRun` and `watch`, and
  then the first of `agentCard`, `calibrate` and `dryRun` that is set wins. `--file` is refused only after `calibrate`
  has returned, so `--calibrate --file` never reaches that check.
- **One check between the loop and the dispatch**, before any settings are loaded or a directory is touched:
  - the modes asked for are `--agent-card`, `--calibrate`, `--dry-run` and `--watch` (the mailbox's own mode,
    once or for good); more than one is a `ConfigException` "`--a` and `--b` cannot run together", in the order
    they were given, so it ends with exit 2;
  - `--file` without `--dry-run` and `--dir` without `--calibrate` move up into the same place, so no mode runs
    before them.
- **No caller is refused by it.** Porelo starts the filter with one command line, `--mail <mailbox root>
  --stabilityDelayMillis 0 [--blocking] --receiptOnApproval true`, and passes none of the four modes or `--file`;
  porelo-message-matrix never starts it.
- **`--agent-card` stays a mode of its own, not one that ignores the rest:** it prints and exits, and a line that
  adds another mode asks for something it will not do. That is the same refusal.
- **The test that goes red first, in `MainTest`:** for each of the six pairs of the four modes, and for
  `--file` or `--dir` beside a mode they do not belong to (`--calibrate --file`, `--agent-card --file`,
  `--agent-card --dir`), exit 2, standard error naming both flags, nothing on standard output, and the mailbox's
  `incoming/` unchanged. Against today's code, a dry run with `--watch` prints a verdict and exits 0 or 1, and
  `--calibrate --file` prints a configuration, so they fail.
  The harm it guards: a command that does something other than what was asked, silently.
- **`doc/filter.md`**, *The command line*: one sentence that the four modes are exclusive and a line asking for
  two exits with 2. **`CHANGELOG.md`**, under `[Unreleased]`, *Changed*.
- **Read against the skills** (graal 2026.910.3; java-code-review, test-quality, security-audit,
  concurrency-review, clean-code and solid-principles 2026.906.3). test-quality found two gaps, both fixed in the
  test and seen red on the code before: `--calibrate --file` is no pair of modes, so the test names what cannot
  run together rather than two modes; and `--agent-card` with `--file` or `--dir` was not covered. clean-code's
  suggestion to hold the four modes in an enum instead of their flag strings is declined: it reads better and
  guards nothing, and the strings are the flags the person types.

## Acceptance

- Each of those nine command lines exits 2 with both flag names on standard error and moves nothing; seen red
  on today's code first.
- The four command lines of `doc/filter.md` still run as before: the existing `MainTest` cases stay green.
