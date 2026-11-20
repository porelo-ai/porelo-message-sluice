# SL58 — The Command Line Only Maps

```json
{
  "$schema": "issue-1.json",
  "id": "SL58",
  "title": "The Command Line Only Maps",
  "state": "Analyzed",
  "size": "S",
  "priority": "Medium",
  "rank": 1,
  "blockedBy": [],
  "covers": "`cli/Main` only parses its arguments, calls one method per mode and prints the answer. The mailbox's use case moves into `MessageSluice`: checking the directories, taking the lock, a pass or a watch tied to the lock. The directories a dry run and a calibration read move into `Settings` and `Calibration`. Each is tested without the command line."
}
```

**Why this size.** The change stays inside `filter/app`: `cli/Main`, `MessageSluice`, `Settings`, `Calibration` and
their tests. No contract changes: the arguments, the exit codes and what is printed stay as they are.

**What must be true.** The filter's only entry point, the command line, checks its arguments, maps them to settings,
calls one method and maps the answer to output and an exit code. The use case behind each mode lives where it can be
tested without a command line, and a second entry point would call the same methods.

## Why

`Main.runChecked` runs the mailbox's use case itself, in this order:

1. `checkDirectories`, then the startup line on standard error;
2. `DirectoryLock.acquire(config.lock())`;
3. `watch(…, lock::stillHeld)` or `runOnce()`.

The rule that a watch ends once its lock is lost exists only because `Main` wires it. Its unit test,
`MessageSluiceTest.aWatchWhoseLockWasRemovedEnds`, wires the lock by hand the same way, so it would not notice if
`Main` stopped doing so. A second instance refused for the lock is tested only through `Main.run`, in
`MainTest.aSecondInstanceRefusesAndNamesTheFirst`.

The other modes work the same way:

- `Main.dryRun` makes up a `mail` directory from the file's parent, so that `Settings.load` passes.
- `Main.calibrate` falls back to the corpus and checks that the directory exists.

## Plan

- **The mailbox, in `MessageSluice`:** a new method `serve(boolean watch, Runnable started, Consumer<RunSummary>
  each)`.
  - It checks the directories, then calls `started`, which is where `Main` prints the startup line today. A broken
    directory is therefore still reported before that line.
  - It acquires `DirectoryLock` on `config.lock()` and runs one pass, or a watch with `lock::stillHeld`.
  - It returns the last `RunSummary`, or none after a watch.
  - `LockedException` and `ConfigException` reach the caller as today.
- **`Main`'s mailbox mode** becomes `Settings.load`, `new MessageSluice`, one call to `serve`, and printing the
  summary and its exit code.
- **The dry run:** `Settings.forFile(configFile, cli, file)` supplies the directories a dry run does not use, in
  place of `Main`'s made-up `mail`. `Main.dryRun` calls it and `MessageSluice.dryRun`.
- **The calibration:** `Calibration.run(@Nullable Path dir)` defaults to the corpus and refuses a missing directory
  with `ConfigException`. `Main.calibrate` keeps only the call and the print.
- **`--agent-card`** already only maps, and stays as it is.
- **Tests:**
  - `aWatchWhoseLockWasRemovedEnds` calls `serve(true, …)`, so the lock wiring is the service's own.
  - A new `MessageSluiceTest` case: a second `serve` while the lock is held is refused and moves nothing.
  - `SettingsTest`: a dry run without mail directories.
  - `CalibrationTest`: the corpus default and a missing directory.
  - `MainTest` stays unchanged and must pass as it is.
- **Red first:** the new `MessageSluiceTest` case of a held lock, written against `serve` before `serve` takes the
  lock.
- **The order with [SL55](README.md#sl55):** that issue changes how `watch` waits, and this issue changes who calls
  it. Whichever is built second rebases on the first. The lock-loss test then becomes that issue's unit test, reached
  through `serve`.

## Acceptance

- `Main` holds no `DirectoryLock`, no `checkDirectories` call, no `watch` or `runOnce` call and no made-up directory.
- Each of these tests fails when its rule is broken on purpose, seen red:
  - `serve`'s refusal under a held lock;
  - the watch ending when the lock is lost, reached through `serve`;
  - a dry run without mail directories;
  - the calibration's corpus default and its refusal of a missing directory.
- `MainTest` passes unchanged: the same arguments give the same output, in the same order, and the same exit codes.

## To be checked

- Whether `Calibration` should take the `FilterConfig` rather than the whole `Settings`, once `Main` no longer
  resolves its directory; it is decided while building, since nothing outside `filter/app` sees it.
