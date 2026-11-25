# The filter

`porelo-message-sluice-filter` reads the messages in a task's mailbox, refuses the ones that carry
data instead of prose, and answers every message with what it found. It has no queue, no network and
no knowledge of any transport: it reads a file, decides, answers and moves the file.

What it looks for in the text is in [`detectors.md`](detectors.md). This page is the frame around
that: the directories, the envelope, the verdict, the answers and the command line.

## The directories

Porelo lays out the mailbox and gives the filter its paths. The filter creates no directory and does
not start when one is missing:

```
<task state>/mail/
├─ incoming/            messages to check, each signed one with <name>.sig beside it
├─ filter/
│   ├─ accepted/        what passed, with its signature; Porelo sends it on from here
│   ├─ feedback/        one answer per message
│   ├─ rejected/        the refused originals, in clear text - 0700
│   └─ error/           files that are not a message, with <name>.error.txt - 0700
└─ sent/                what was sent before, where a repeated message id is looked up
```

`--mail <dir>` names them all at once. **Every one must be a real directory, not a link**, and the
filter remembers what each one is - its device and inode - and checks it again before every pass: a
directory replaced since the start, or reached through a parent that was, stops the pass before
anything is read or filed. **`rejected/` and `error/` must also be `0700`**, owned by the filter's
user: they hold what it refused, secrets included.

**What is in `accepted/` counts as checked and is never checked again**, so nothing but the filter may
write there.

## Per message

- Only `*.json` files are read; a file still being written is left for the next pass.
- Only a plain file is read: a link, a second hard link or a special file is refused unread. The same
  holds where the filter looks up message ids already seen: there, anything but a plain file of at most
  a message's size counts for nothing.
- **The file name** is 1 to 128 letters, digits, `.`, `_` and `-`, starts with a letter or digit and
  ends in `.json`. It travels with the message, so the detectors check it like a text.
- **The signature** beside it, when there is one, must have exactly the shape Porelo writes (an OpenSSH Ed25519 signature,
  at most 1 KiB), so nothing can ride along in it. The filter checks the shape; Porelo checks the
  signature itself.
- The JSON is parsed strictly, and **only strict UTF-8** is read.

**The filter never changes a message.** What it files is exactly the bytes it checked, because those
are the bytes Porelo signs and sends. **Every message gets an answer**, written before the message is
filed, so a crash leads at worst to a second answer, never to a message dropped in silence. A message
a crash left in `incoming/` after it was filed - the same bytes, under the same name, in `accepted/` -
is taken as filed and removed on the next pass, not refused as its own duplicate. A message and its
signature are removed only together.

## The envelope

A message is an **A2A 1.0** `Message` or `Task` in its JSON form; a task's status, artifacts and history are
checked like a message. A2A 0.3 is refused, never converted.

| What | Rule | Severity |
|---|---|---|
| a part with `raw` | `ENVELOPE/RAW_PART_NOT_ALLOWED` | `BLOCKING` |
| a part with `url` | `ENVELOPE/URL_PART_NOT_ALLOWED` | `BLOCKING`, or `SUSPICIOUS` by `check.urlPartSeverity` |
| a part with `data`, but in a review comment | `ENVELOPE/DATA_PART_NOT_ALLOWED` | `BLOCKING` |
| a review comment of other parts than one text part or one data part | `ENVELOPE/COMMENT_PARTS_MIXED` | `BLOCKING` |
| a review comment a task sends to anyone but `review` | `ENVELOPE/COMMENT_NOT_TO_REVIEW` | `BLOCKING` |
| a part with two content fields | `ENVELOPE/AMBIGUOUS_PART` | `BLOCKING` |
| a part with none | `ENVELOPE/EMPTY_PART` | `SUSPICIOUS` |
| a role that is not allowed | `ENVELOPE/UNEXPECTED_ROLE` | `BLOCKING` |
| `filename` on a text part | `ENVELOPE/FILENAME_ON_TEXT_PART` | `SUSPICIOUS`, `BLOCKING` for a binary extension |
| a `mediaType` other than `text/plain` | `ENVELOPE/UNEXPECTED_MEDIA_TYPE` | `SUSPICIOUS`, `BLOCKING` for octet-stream, zip or JSON |
| an id too long or not `[A-Za-z0-9._:-]` | `ENVELOPE/ID_MALFORMED` | `BLOCKING` |
| `metadata.to` or `.from` neither a plain name (letters, digits, `.` `_` `-` `@`) nor a task of another project, `task:<project>:<task>` | `ENVELOPE/PEER_NAME_MALFORMED` | `BLOCKING` |
| metadata too deep or too large | `ENVELOPE/METADATA_TOO_DEEP`, `…_TOO_LARGE` | `BLOCKING` |
| too many parts, a text too long | `ENVELOPE/TOO_MANY_PARTS`, `…/TEXT_TOO_LONG` | `BLOCKING` |
| too many `referenceTaskIds` | `ENVELOPE/TOO_MANY_REFERENCES` | `BLOCKING` |
| an extension that is not a URI | `ENVELOPE/EXTENSION_MALFORMED` | `SUSPICIOUS` |
| a field A2A 1.0 does not have | `ENVELOPE/UNKNOWN_FIELD` | `SUSPICIOUS` |
| a message id seen before | `ENVELOPE/DUPLICATE_MESSAGE_ID` | `BLOCKING` |

**A review comment** (`metadata.kind` `comment` on the outer message) is either a reply in prose, one
text part, or one review document in one `data` part. The document is checked as the next section
says; every other message's data part is refused.

**Every string goes to the detectors**, not only the text parts: file names, media types, every key
and value of `metadata`, extensions, unknown fields and the ids. A UUID passes as an id; a long hex
string does not, whatever field it is in.

**Roles:** a message may be the agent's (`ROLE_AGENT`) or a person's (`ROLE_USER`); both are checked
alike. That a person really wrote it is Porelo's to check, not the filter's.

**A file that is not a message** - empty, not UTF-8, not JSON, not A2A 1.0 - goes to `error/` and still
gets an answer, so a broken file is never a way to make a message vanish. What `error/` keeps is the
bytes that were read, never the file they came from; a file changed since stays for the next pass. A
link or a special file is not read, so it is moved as it is, a link as a link, never followed.

## A review document

A comment's data part is one review profile 2 document, as Porelo's `doc/review-profile.md` and its
schema `review-profile-2.schema.json` write it. The filter walks it as a closed tree, with no data
binding and no SARIF library, and refuses what the profile does not list. **Every rule here is
`BLOCKING` and an envelope rule**, so it refuses also when the host runs the check reporting only;
what a detector finds in the document's texts is weighed like any other text.

**The direction is the host's to name**, on the command line: without `--inbound` the message is what
a task sends, and its document must be a submission (`porelo/documentKind` `submission`) addressed to
`review` that names no producer; with `--inbound` it comes from a forge, and its document must be a
page whose every result names the forge account that wrote it (`porelo/sourceKind` `forge-account`,
`porelo/forgeAccountId`, `porelo/forgeAccountName`) and the time the forge says it wrote it
(`porelo/createdAt`, an instant in UTC in whole seconds, `2026-11-25T10:15:30Z`, a real calendar
instant with no hour 24 and no second 60). A host that forgets the flag refuses a forge's
page rather than passing it outbound. A configuration file cannot name the direction.
**Either way the document is a pull request's**: its run names `porelo/pullRequest` and the forge's
repository URL, never a push of Porelo's. Both URLs keep profile 2's forge-neutral shape: a pull request
run's `repositoryUri` is `https://<host>[:<port>]` and 2 to 8 path segments, and its `porelo/pullRequest` that
URL and 1 to 3 more segments, the last all digits (`pull/42`, `pulls/42`, `-/merge_requests/42`); each segment
1 to 100 of `A-Z`, `a-z`, `0-9`, `.`, `_`, `~` and `-`, neither `.` nor `..`, so no percent-encoding, no empty
segment and no trailing slash. The filter also holds the host to DNS labels and the port to 1 to 65535.

| What | Rule |
|---|---|
| a document over a parser budget: 1 MiB, 32 KiB per result, 100 results, nesting 24, 100000 tokens, strings of 16384 characters, names of 128, numbers of 20 digits | `PROFILE/BUDGET_EXCEEDED` |
| a field profile 2 does not list | `PROFILE/UNKNOWN_FIELD` |
| a field it requires, missing | `PROFILE/MISSING_FIELD` |
| a value of the wrong type, word or grammar | `PROFILE/VALUE_MALFORMED` |
| a format character, variation selector, line or paragraph separator, lone surrogate or control character other than line feed and tab, anywhere | `PROFILE/FORBIDDEN_CHARACTER` |
| a submission sent inbound, or a page sent outbound | `PROFILE/WRONG_DIRECTION` |
| a producer, guid, thread, thread state or time of writing in a submission; a page's result by anyone but a forge account, without its account's id and name, or naming a task | `PROFILE/PRODUCER_NOT_ALLOWED` |
| a region of more than 100 lines | `PROFILE/REGION_TOO_LARGE` |
| a replacement outside its comment's lines, or more than 10 lines longer | `PROFILE/REPLACEMENT_OUTSIDE_ANCHOR` |
| a text over 8192 bytes, a replacement over 8 KiB, a replaced line over 400 characters | `PROFILE/TEXT_TOO_LONG` |

**What the detectors see of it:** `message.text` as prose; the suggested replacement, each file's
path, each URL's host, its path segments one by one and its whole path once more, the thread ids and the forge account's id and name as code (see [the
detectors](detectors.md)). The guid, the two commit ids and a result's time of writing are held to their grammar alone, since a
full commit id trips the encoding rules by design, and so are a URL's port and the pull request's
number. **A field name is never shown**: a finding names an
unknown field by its position, `#3`, as its writer chose the name.

## The verdict

Findings are `INFO`, `SUSPICIOUS` or `BLOCKING`, and no detector decides on its own:

- **`THRESHOLD`** (the default): one `BLOCKING` finding refuses, and so do `SUSPICIOUS` findings from
  two different rules (`decision.suspiciousCombineCount`).
- **`SCORE`**: findings are weighed (`decision.weight.<rule>`), and the sum refuses from
  `decision.scoreThreshold`.
- **Always refused**, whatever is configured: a `BLOCKING` envelope finding, a detector that failed,
  and a finding that could not be masked.
- Detector rules can be switched off (`decision.disabledRules`) and their categories re-graded
  (`decision.severity.<category>`).

**Reporting or blocking.** With `--blocking` the verdict above refuses. Without it the filter only
reports: only the envelope refuses, and each answer says what blocking would have refused
(`wouldReject: true`). **Porelo runs it blocking**, for what a task sends and for what arrives from a
peer it does not vouch for. A project may set its outgoing check to report only, on purpose
(`mail.outgoing_filter: reporting`); Porelo then holds every message the filter would have refused,
and every message whose answer is missing, until a person releases it, and shows the verdict there.

## The answers

Every message gets one answer in `feedback/`: a **receipt** when it passed (`receiptOnApproval=false`
switches these off, but never for a message blocking would have refused: its receipt is how the host
learns it was flagged), a **rejection** when it did not. An answer is itself an A2A 1.0 message:

- a text part listing every finding: severity, rule, where, and the reason in English;
- a `data` part with the decision, the mode, the deciding rule and every finding, for a program to
  read.

**Nothing a rule matched appears in clear text** - not in an answer, a log line or a file name.
`redaction.mode` is `PARTIAL` (the default: up to 12 stars, after the first four characters of a
match of 16 or more, each shown only if it is an ASCII letter or digit, else `?`), `FULL` (`[REDACTED:<category>]`)
or `HASH`. A file kept in `rejected/` or `error/` loses its name when the name itself was suspicious.
A key of `metadata` above a finding, and a field the envelope reports, is named by its position
(`metadata.#0`, `#3`), never in clear: a name is text its writer chose.

## The command line

```
porelo-message-sluice-filter --mail <dir> [--blocking] [--inbound] [--watch] [--config <file>] [--<key> <value> ...]
porelo-message-sluice-filter --dry-run --file <message> [--blocking] [--inbound]   # checks one file, moves nothing
porelo-message-sluice-filter --calibrate [--dir <messages>]            # prints a configuration
porelo-message-sluice-filter --agent-card --mail <dir>                 # prints its A2A agent card
```

**`--inbound`** says the messages come from a peer the host does not vouch for, such as a forge;
without it they are what a task sends. It decides only which review document a comment may carry.

**`--agent-card`, `--calibrate`, `--dry-run` and `--watch` exclude each other:** a command line asking
for two of them, or for `--file` or `--dir` without its mode, runs nothing and exits with 2.

**Exit codes:** 0 everything passed, 1 something was refused, 2 a technical error or a configuration
that cannot be used. One instance runs per mailbox; a second exits with 2. `--watch` keeps running
and checks whenever a file arrives; it ends with 2 when the lock file `filter/.lock` was removed or
replaced, since a second instance could then lock a new one. **Whatever the exit code, `accepted/` holds only what was checked
and passed:** a file that met a technical error is not judged, nothing of it is filed, and it stays in
`incoming/` for the next pass. Exit 2 puts nothing in `accepted/` in doubt.

**Configuration**, later beating earlier: the defaults, `--config <file>` (a properties file), and
`--<key> <value>`. **The environment is never read**, so nothing a shell exported can switch a rule
off. **An unknown key stops the start**, and the configuration in force is logged at start, each key
with where its value came from. The detectors' keys are in [`detectors.md`](detectors.md).

**Porelo's limit factors** come as its limits line, `--limits services=1.0,machines=1.0,polling=1.0,retries=1.0`,
or as the key `limits` in the properties file. Only `polling` has a use here: `stabilityDelayMillis` (200) and
`watchIntervalMillis` (2000) take effect times it, rounded up to a whole millisecond, so `polling=2` looks half as
often. The other three are read and checked but change nothing, since the filter calls no outside service, starts
no program and retries nothing. Without a line all four are 1.0, and a name left out of a line is 1.0; an empty
line, an unknown or repeated name, or a value that is not a decimal from 0.1 to 100 with at most two digits after
the dot, stops the start: given with `--limits`, with exit 64
and `--limits: <what is wrong>`, in the properties file with exit 2.

## What the operator has to guarantee

1. **The agent has no other way out.** A filter the agent can send around checks only part of what
   leaves.
2. **Nothing but the filter writes into `accepted/`**, and nothing but Porelo's sending takes from it.

## Known limits

| Limit | What would lift it |
|---|---|
| A secret described in words (*"the dog's name plus 77"*) | only a model, which this filter refuses to be |
| Information hidden in word choice | out of scope |
| A2A 1.0 only | a lossless conversion in front |
| Files only, no A2A protocol operation | a transport adapter; the checks would stay the same |
| A repeated message id is found only among this mailbox's files | Porelo's own check, by id |
