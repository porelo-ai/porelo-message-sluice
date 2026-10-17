# Issues

What is open in this repository, ordered by what to do next rather than by number. The number is
identity, not sequence.

**Grouped by state**, each group's heading naming it, as the shared rules set them out; within a group
the order is the priority. Only the operator sets Accepted, Rejected, Estimated, Backlog, Rework and
Finished; this repository's agent sets Analyzed, Implementing and Review, and proposes the rest in the
channel.

## Implementing

| # | Size | Blocked by | What it covers |
|---|---|---|---|
| <a id="sl26"></a>[SL26](SL26-Sokar-Is-Renamed-Porelo-Here.md) | S | — | The rename to Porelo here: the coordinates, the paths Porelo reads, the signature's namespace, a fixture signed again, and a test that refuses the old name. |

## Analyzed

| # | Size | Blocked by | What it covers | Open questions |
|---|---|---|---|---|
| <a id="sl25"></a>[SL25](SL25-The-Code-Changed-Since-The-Skills-Reading.md) | M | — | The code changed since the reading of 2026-09-27 (`ddea1c3`), read against the seven skills, after Phase 0's rename and code style window. | 0 |

## Estimated

| # | Size | Blocked by | What it covers |
|---|---|---|---|
| <a id="sl07"></a>[SL07](SL07-Refuse-Credentials-And-Personal-Data.md) | L | — | Credentials and personal data, caught by a catalog of pattern, context and checksum. |
| <a id="sl04"></a>[SL04](SL04-An-External-Detector-Measured.md) | L | — | Somebody else's detector catalog behind the interface, measured before anything is adopted. |
| <a id="sl23"></a>[SL23](SL23-The-Narrowed-Message-Schema-The-Filters-Side.md) | M | Porelo [B108](https://github.com/porelo-ai/porelo/blob/main/issues/base/README.md#b108) | [B14](https://github.com/porelo-ai/porelo/blob/main/issues/base/README.md#b14)'s narrowed message schema enforced in the envelope: four `metadata` keys, five kinds with their fixed data parts, one text part, one extension URI. |
| <a id="sl24"></a>[SL24](SL24-A-Code-Comment-The-Filters-Side.md) | L | Porelo [B166](https://github.com/porelo-ai/porelo/blob/main/issues/base/README.md#b166), [SL23](#sl23) | A comment on code passes only in its exact shape: an anchor checked by shape, prose judged as prose, an optional suggestion bounded to its anchor and judged by detectors measured on code. |
| <a id="sl08"></a>[SL08](SL08-Refuse-A-Payload-Spread-Over-Several-Messages.md) | XL | — | A payload cut into pieces and sent over several messages, caught by correlating against what the task already sent. |
| <a id="sl12"></a>[SL12](SL12-Refuse-A-Description-That-Stopped-Being-True.md) | S | — | A test that refuses a description contradicted by something the build can read - the modules named against the modules that exist. |

Nothing is in Review, Rework, Backlog, Accepted, Idea, Finished or Rejected.

**Open questions**, in *Analyzed* only, counts what waits on the operator; what this repository's
agent finds out itself stands under each issue's *To be checked* and is not counted.

**Size** is how big it is, read in the code: XS under an hour, S a few hours, M about a day, L several
days, XL too big to build as one, split in its plan.

**Blocked by** names what an issue waits for: an issue here by its number, a Porelo requirement as
`Porelo B<n>`.

## One module, one job

`filter/` decides what may pass and knows no transport. The transports that carry what passed live
in repositories of their own; what the filter shares with them is the directory contract and the
message format, both written down rather than compiled against.

## Where Porelo comes in

Nothing here is blocked by Porelo: this is a program that reads and writes directories. **Porelo
requirement [B14](https://github.com/porelo-ai/porelo/blob/main/issues/base/README.md#b14)** is the other half - the mailbox mounted into a task, who feeds this filter, which
transport a peer uses, and what may be in a message in the first place.
