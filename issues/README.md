# Issues

What is open in this repository, ordered by what to do next rather than by number. The number is
identity, not sequence.

**Grouped by state**, each group's heading naming it, as the shared rules set them out; within a group
the order is the priority. Only the operator sets Accepted, Rejected, Estimated, Backlog, Rework and
Finished; this repository's agent sets Analyzed, Implementing and Review, and proposes the rest in the
channel.

## Review

| # | Size | Blocked by | What it covers |
|---|---|---|---|
| <a id="sl27"></a>[SL27](SL27-Reformatted-To-The-Shared-Style.md) | S | porelo-project [PJ29](https://github.com/porelo-ai/porelo-project/blob/main/issues/README.md#pj29) | This repository's share of the shared code style: Spotless on, once `porelo-parent` `0.2.1-SNAPSHOT` has it, one reformatting commit, and the shared `.editorconfig`. |
| <a id="sl25"></a>[SL25](SL25-The-Code-Changed-Since-The-Skills-Reading.md) | L | [SL27](#sl27) | The code changed since the reading of 2026-09-27 (`ddea1c3`), read against the seven skills on the reformatted code, its findings fixed without changing an interface another repository reads. |

## Accepted

| # | Size | Blocked by | What it covers |
|---|---|---|---|
| <a id="sl28"></a>[SL28](SL28-Modes-That-Cannot-Go-Together-Are-Refused.md) | — | — | A command line asking for two modes that cannot run together is refused by name, not run as the first of them. |

## Estimated

| # | Size | Blocked by | What it covers |
|---|---|---|---|
| <a id="sl07"></a>[SL07](SL07-Refuse-Credentials-And-Personal-Data.md) | L | — | Credentials and personal data, caught by a catalog of pattern, context and checksum. |
| <a id="sl04"></a>[SL04](SL04-An-External-Detector-Measured.md) | L | — | Somebody else's detector catalog behind the interface, measured before anything is adopted. |
| <a id="sl23"></a>[SL23](SL23-The-Narrowed-Message-Schema-The-Filters-Side.md) | M | Porelo [B108](https://github.com/porelo-ai/porelo/blob/main/issues/base/README.md#b108) | [B14](https://github.com/porelo-ai/porelo/blob/main/issues/base/README.md#b14)'s narrowed message schema enforced in the envelope: four `metadata` keys, five kinds with their fixed data parts, one text part, one extension URI. |
| <a id="sl24"></a>[SL24](SL24-A-Code-Comment-The-Filters-Side.md) | L | Porelo [B166](https://github.com/porelo-ai/porelo/blob/main/issues/base/README.md#b166), [SL23](#sl23) | A comment on code passes only in its exact shape: an anchor checked by shape, prose judged as prose, an optional suggestion bounded to its anchor and judged by detectors measured on code. |
| <a id="sl08"></a>[SL08](SL08-Refuse-A-Payload-Spread-Over-Several-Messages.md) | XL | — | A payload cut into pieces and sent over several messages, caught by correlating against what the task already sent. |
| <a id="sl12"></a>[SL12](SL12-Refuse-A-Description-That-Stopped-Being-True.md) | S | — | A test that refuses a description contradicted by something the build can read - the modules named against the modules that exist. |

## Finished

| # | Title | What was built |
|---|---|---|
| <a id="sl26"></a>[SL26](README.md#sl26) | The rename to Porelo here | The filter is published as `org.fuin.porelo` 0.5.0 and installed where Porelo looks for it, accepts only signatures in the namespace `porelo-message`, and a documents test refuses the old name in any tracked file. |

Nothing is in Rework, Implementing, Backlog, Analyzed, Idea or Rejected.

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
