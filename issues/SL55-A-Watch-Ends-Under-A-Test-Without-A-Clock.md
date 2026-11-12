# SL55 — A Watch Ends Under A Test Without A Clock

```json
{
  "$schema": "issue-1.json",
  "id": "SL55",
  "title": "A Watch Ends Under A Test Without A Clock",
  "state": "Estimated",
  "size": "S",
  "priority": "Medium",
  "rank": 6,
  "blockedBy": [],
  "covers": "`watch` waits for its next event through a small injected interface, so its three ends - interrupted, a failed pass, a lost lock - are unit tests with a wait that returns at once; one real-thread test stays."
}
```

**Why this size.** One small interface in `MessageSluice.watch` and three tests rewritten, in `filter/app` alone.

**What must be true.** When a watch ends - interrupted, after a failed pass, or because its lock was removed - is
proven without a thread, a poll or a timeout; only one test runs the real `WatchService` on a real thread.

## Why

`watchingRunsAPassWhenAFileArrivesAndStopsWhenInterrupted`, `aPassThatFailsOnADirectoryEndsTheWatch` and
`aWatchWhoseLockWasRemovedEnds` start a real thread, let it poll every 50 ms and join it for up to 10 s. `watch`
polls its `WatchService` directly, while the stability delay already waits through the injected
`MessageSluice.Pause`.

## Plan

- `watch` waits for the next event through a small injected interface instead of calling `WatchService.poll`
  directly; the production wait wraps the `WatchService` as today.
- The three ends become unit tests with a wait that returns at once.
- One test against the real `WatchService` and thread stays.
- Red first: a unit test of a lost lock against a lock check broken on purpose.

## Acceptance

- Each of the three ends fails its unit test when broken on purpose, seen red.
- No test but the one real-thread watch test waits on a thread, a poll or a timeout to see a flow work.

## Decided by the operator (2026-11-12)

- The test pyramid audit is built as three issues: [SL53](README.md#sl53) for the per-message decision,
  [SL54](README.md#sl54) for the envelope and the answer, and this one.
