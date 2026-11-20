# SL58 — The Command Line Only Maps

```json
{
  "$schema": "issue-1.json",
  "id": "SL58",
  "title": "The Command Line Only Maps",
  "state": "Idea",
  "size": "S",
  "priority": "Medium",
  "rank": 1,
  "blockedBy": [],
  "covers": "`cli/Main` only parses its arguments, calls one service method per mode and prints the answer; taking the mailbox lock, checking the directories, choosing a pass or a watch, and finding the directories a dry run and a calibration read move into the service and are tested without the command line."
}
```

**Why this size.** It touches `cli/Main` and one service class in `filter/app` alone, with their tests, and changes no
contract: the arguments, the exit codes and what is printed stay as they are.

**What must be true.** The filter's only entry point, the command line, checks its arguments, maps them to settings,
calls one service method and maps the answer to output and an exit code. The use case behind each mode lives in a
service that knows no command line and is tested without one.

## Why

`Main.runChecked` runs the mailbox use case itself. It checks the directories, takes the mailbox's `DirectoryLock`,
chooses between `watch` and `runOnce`, and ties the watch to the lock still being held. `Main.dryRun` makes up a
`mail` directory so that `Settings.load` passes, and `Main.calibrate` falls back to the corpus and checks that the
directory exists. A second entry point would have to repeat all of this, and today it is tested only through
`Main.run`.

## Plan

- One service method per mode, on `MessageSluice` or a small class beside it:
  - the mailbox, which checks the directories, holds the lock and runs one pass or a watch;
  - the dry run of one file, needing no mail directory;
  - the calibration of a directory, defaulting to the corpus.
- `Main` keeps:
  - parsing the arguments and refusing modes that cannot run together;
  - `Settings.load`;
  - the startup line on standard error;
  - mapping each result or exception to output and an exit code.
- `--agent-card` already only maps, and stays as it is.
- Red first: a service test that runs the mailbox while another holder has the lock, and expects the refusal
  without `Main`.

## Acceptance

- `Main` holds no `DirectoryLock`, no `checkDirectories` call, no `watch` or `runOnce` call and no made-up directory.
- Each of these service tests fails when its rule is broken on purpose, seen red:
  - the mailbox's lock refusal;
  - the watch ending when the lock is lost;
  - a dry run without mail directories;
  - the calibration's corpus default.
- The command line's existing tests pass unchanged: the same arguments give the same output and exit codes.
