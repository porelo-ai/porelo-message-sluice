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
| <a id="sl30"></a>[SL30](SL30-Old-Runs-Deleted-On-A-Named-Runner.md) | XS | — | *Delete old runs* and the *Shared rules check* on `ubuntu-26.04`. |
| <a id="sl31"></a>[SL31](SL31-Publish-Without-The-JFrog-Action.md) | S | — | Publish reaches Artifactory with `curl` instead of `jfrog/setup-jfrog-cli`. |
| <a id="sl12"></a>[SL12](SL12-Refuse-A-Description-That-Stopped-Being-True.md) | S | — | A test that refuses a description contradicted by something the build can read - the modules named against the modules that exist. |
| <a id="sl07"></a>[SL07](SL07-Refuse-Credentials.md) | M | — | Credentials - keys, tokens, private keys, connection strings, passwords - caught by a catalog of pattern, validation and context. |
| <a id="sl32"></a>[SL32](SL32-Refuse-Personal-And-Financial-Data.md) | M | — | Payment cards, IBANs, national ids refused; e-mail, phone, IP address and date of birth suspicious; by pattern and checksum. |

## Estimated

| # | Size | Blocked by | What it covers |
|---|---|---|---|
| <a id="sl04"></a>[SL04](SL04-An-External-Detector-Measured.md) | L | — | Somebody else's detector catalog behind the interface, measured before anything is adopted. |
| <a id="sl23"></a>[SL23](SL23-The-Narrowed-Message-Schema-The-Filters-Side.md) | M | Porelo [B108](https://github.com/porelo-ai/porelo/blob/main/issues/base/README.md#b108) | [B14](https://github.com/porelo-ai/porelo/blob/main/issues/base/README.md#b14)'s narrowed message schema enforced in the envelope: four `metadata` keys, five kinds with their fixed data parts, one text part, one extension URI. |
| <a id="sl24"></a>[SL24](SL24-A-Code-Comment-The-Filters-Side.md) | L | Porelo [B166](https://github.com/porelo-ai/porelo/blob/main/issues/base/README.md#b166), [SL23](#sl23) | A comment on code passes only in its exact shape: an anchor checked by shape, prose judged as prose, an optional suggestion bounded to its anchor and judged by detectors measured on code. |
| <a id="sl08"></a>[SL08](SL08-Refuse-A-Payload-Spread-Over-Several-Messages.md) | XL | — | A payload cut into pieces and sent over several messages, caught by correlating against what the task already sent. |

## Finished

| # | Title | What was built |
|---|---|---|
| <a id="sl29"></a>[SL29](README.md#sl29) | Built in a Porelo task | `porelo.yml` names the tools of the whole local check, and that check, native images and both packages included, ran green inside a Porelo task built from it. |
| <a id="sl28"></a>[SL28](README.md#sl28) | Modes that cannot go together are refused | A command line asking for two of the four modes, or for `--file` or `--dir` without its mode, runs nothing and exits 2 naming both. |
| <a id="sl25"></a>[SL25](README.md#sl25) | The code changed since the skills reading | The code changed since 2026-09-27 read against the seven skills and `shellcheck`, its sixteen findings fixed (each that guards a harm with a test seen red), published as `0.5.1~snapshot.51`. |
| <a id="sl27"></a>[SL27](README.md#sl27) | Reformatted to the shared style | Spotless with the shared Java, YAML and shell formats, one reformatting commit that `git blame` skips, the shared `.editorconfig`, and checkouts without a kept token. |
| <a id="sl26"></a>[SL26](README.md#sl26) | The rename to Porelo here | The filter is published as `org.fuin.porelo` 0.5.0 and installed where Porelo looks for it, accepts only signatures in the namespace `porelo-message`, and a documents test refuses the old name in any tracked file. |

Nothing is in Rework, Implementing, Backlog, Analyzed, Accepted, Idea or Rejected.

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
