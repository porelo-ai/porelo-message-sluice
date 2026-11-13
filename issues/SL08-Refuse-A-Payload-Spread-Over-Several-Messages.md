# SL08 — Refuse a payload spread over several messages

```json
{
  "$schema": "issue-1.json",
  "id": "SL08",
  "title": "Refuse a payload spread over several messages",
  "state": "Estimated",
  "size": "XL",
  "priority": "Low",
  "rank": 3,
  "blockedBy": [],
  "covers": "A payload cut into pieces and sent over several messages, caught by correlating against what the task already sent."
}
```

**Why this size.** a corpus index, seven rules and a reconstruction; it splits into three issues of L or
smaller, as the plan says.

**What must be true.** A payload cut into pieces that are each unremarkable and sent by one task over
several messages is refused once the pieces add up, and the refusal names every message involved.

## Why

This is the third of the three things the filter exists to catch: no single message is refused by
the encoded-data checks or by the filter's credential detectors, yet together they are a file. A payload cut into
pieces inside one message is already caught ([`doc/detectors.md`](../doc/detectors.md)); this is the
correlation across messages. How it would be built is
*Design* below.

## The shape

- Messages are grouped as the design says: by `contextId`, then `taskId`, then `referenceTaskIds`, by a time window
  when none is present; `metadata.thread` is no key of it. They are correlated against **what this task
  has already sent**, and caught at the latest when the accumulated size crosses the threshold.
- The corpus lives with the task's mailbox and dies with it. **The accepted gap:** a sender that
  spreads its pieces over two tasks it started itself is seen by neither.
- A persistent index over the corpus is written forward and rebuildable at any time; it is a cache,
  never the source of truth, and holds fragment metadata only, never full text.
- What one check reads is capped, so the cost per message does not grow with the life of the task.
- **The detector is built-in**: correlation cannot be delegated to an external engine invoked once
  per message, as measured in [SL04](README.md#sl04).

## Design

A proposal: **nothing here exists yet**, and none of it has been measured.

### Correlation across the context

An attacker can cut a file or a secret into pieces that are each unremarkable. `sent/` holds
everything accepted so far, and A2A's `contextId`/`taskId` give the grouping for free, which makes
this far more solid than a heuristic over file names.

Grouping is `contextId`, then `taskId`, then `referenceTaskIds`, falling back to a time window when
none is present. Order is arrival order in `sent/`, refined by explicit sequence markers in the
text (`part 3/12`). The fragment index per accepted message holds only fragment metadata - offset,
length, alphabet, entropy, hash - never full text.

| Rule | What it catches |
|---|---|
| `AccumulatedPayloadRule` | the sum of suspicious fragment characters per context: 20 characters are harmless, 40 × 20 are a file. Default threshold 512. |
| `ConsistentAlphabetRule` | fragments across messages of one context using exactly the same alphabet throughout |
| `ReconstructionRule` | the fragments concatenated in the determined order, decoded, and checked for **magic bytes** (`MZ`, `ELF`, `PK`, `%PDF`, `\x89PNG`), a PEM block or a secret pattern - the refusal then names every `messageId` involved |
| `SplitSecretRule` | a secret that only exists once assembled: PEM header in message 7, body in 8-14, footer in 15; or an AWS key id and its secret key in separate messages |
| `SequenceMarkerRule` | explicit continuation markers beside suspicious fragments; also supplies the ordering |
| `NearDuplicateRule` | many messages with nearly identical sentence structure and a changing payload share - a script rather than a dialogue, found with SimHash |
| `BurstRateRule` | a conspicuous cluster in time; `SUSPICIOUS` only, never blocking on its own |

What one check reads is capped (default: the last 500 messages, or 30 days).

### Configuration

| Parameter | Default | Where it comes from |
|---|---|---|
| `accumulatedPayloadThreshold` | 512 | from there on a meaningful file transfer is possible |
| `contextHistoryLimit` / `MaxAge` | 500 messages / 30 days | bounds the work per check, not the corpus |

`--rebuild-index` rebuilds the index from the corpus and exits. The index lives in
`filter/.index/`, is a cache, and is never what a verdict is read from when it disagrees with the
corpus.

### Known limits, and what each would take

| Limit | What would lift it |
|---|---|
| Correlation only over `contextId`/`taskId` or a time window | sender identity from `metadata` as a further grouping key |
| Reconstruction tries few orderings | greedy reconstruction along decodable fragment boundaries |
| The corpus grows for the life of the task | partition it by date; the index stays the working structure |
| Pieces spread over two tasks of the same operator | a corpus shared across tasks - which would make one task's traffic readable to the check of another, so it is an accepted gap |

## Plan

- **Not a `ContentDetector`:** that interface is stateless and sees one text. Correlation is a stage of
  its own in `MessageSluice.process`, after the envelope and the content checks, reading the corpus
  (`config.corpus()`, today read only for message ids by `io/MessageArchive`).
- **Split into three, each its own issue when started:**
  1. **L - the corpus and the accumulated payload:** the fragment index (`filter/.index/`, metadata
     only) written forward and rebuilt by `--rebuild-index` (`cli/Main`), the grouping by `contextId`,
     `taskId`, `referenceTaskIds` and the time window, the history cap, `AccumulatedPayloadRule` and `ReconstructionRule`.
  2. **M - pieces that only add up as a secret:** `SplitSecretRule`, `SequenceMarkerRule`,
     `ConsistentAlphabetRule`, on the index of the first.
  3. **M - a script rather than a dialogue:** `NearDuplicateRule` and `BurstRateRule`, `SUSPICIOUS` only.
- **Nothing changes for Porelo:** it reads only the filter's exit code and dispatches `accepted/`, and
  matches an answer only to its `rejectedMessageId`; earlier messages are recorded as sent and never
  re-read. A refusal that names earlier, already delivered messages reaches the agent and the person
  as words, in the answer's text, and the directory contract stays as it is.
- **Red first, for the first:** a base64 ZIP spread over five messages of one task - each passes today
  through `Mailbox` in `MessageSluiceTest`; the fifth must be refused naming all five.

## Acceptance

- A ZIP or a PEM file, base64-encoded and spread over 2, 5, 20 and 100 messages of one task, is
  refused at the latest when the threshold is crossed.
- **200 real English messages** in one context produce no correlation finding at all.
- Scenario directories with a pre-filled corpus - with and without sequence markers, with noise
  messages in between, and with messages that have no `contextId` - each have a stated outcome, and
  each test is seen to fail with the detector switched off.
- The index deleted between two runs gives the same verdicts after it is rebuilt.

## To be checked

- The thresholds (512 characters, 500 messages or 30 days) - measured against the 200 real messages
  of the acceptance.
- Who makes `filter/.index/`: `doc/filter.md` says the filter creates no directory and does not start when one is
  missing, so Porelo lays it out or that sentence changes.
