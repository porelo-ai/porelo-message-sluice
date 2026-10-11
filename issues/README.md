# Issues

What is open in this repository, ordered by what to do next rather than by number. The number is
identity, not sequence.

**Grouped by state**, each group's heading naming it, as the shared rules set them out; within a group
the order is the priority. Only the operator sets Accepted, Rejected, Estimated, Backlog, Rework and
Finished; this repository's agent sets Analyzed, Implementing and Review, and proposes the rest in the
channel.

## Analyzed

| # | Size | Blocked by | What it covers | Open questions |
|---|---|---|---|---|
| [SL07](SL07-Refuse-Credentials-And-Personal-Data.md) | L | — | Credentials and personal data, caught by a catalog of pattern, context and checksum. | 2 |
| [SL23](SL23-The-Narrowed-Message-Schema-The-Filters-Side.md) | M | Sokar B108 | B14's narrowed message schema enforced in the envelope: four `metadata` keys, five kinds with their fixed data parts, one text part, one extension URI. | 2 |
| [SL24](SL24-A-Code-Comment-The-Filters-Side.md) | L | Sokar B166 | A comment on code passes only in its exact shape: an anchor checked by shape, prose judged as prose, an optional suggestion bounded to its anchor and judged by detectors measured on code. | 3 |
| [SL08](SL08-Refuse-A-Payload-Spread-Over-Several-Messages.md) | XL | — | A payload cut into pieces and sent over several messages, caught by correlating against what the task already sent. | 2 |
| [SL04](SL04-An-External-Detector-Measured.md) | L | — | Somebody else's detector catalog behind the interface, measured before anything is adopted. | 2 |
| [SL12](SL12-Refuse-A-Description-That-Stopped-Being-True.md) | S | — | A test that refuses a description contradicted by something the build can read - the modules named against the modules that exist. | 2 |

Nothing is in Review, Rework, Implementing, Backlog, Accepted, Idea, Estimated, Finished or Rejected.

**Size** is how big it is, read in the code: XS under an hour, S a few hours, M about a day, L several
days, XL too big to build as one, split in its plan.

**Blocked by** names what an issue waits for: an issue here by its number, a Sokar requirement as
`Sokar B<n>`.

## One module, one job

`filter/` decides what may pass and knows no transport. The transports that carry what passed live
in repositories of their own; what the filter shares with them is the directory contract and the
message format, both written down rather than compiled against.

## Where Sokar comes in

Nothing here is blocked by Sokar: this is a program that reads and writes directories. **Sokar
requirement B14** is the other half - the mailbox mounted into a task, who feeds this filter, which
transport a peer uses, and what may be in a message in the first place.
