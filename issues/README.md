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
| <a id="sl36"></a>[SL36](SL36-Snapshots-Resolved-With-A-Read-Only-Token.md) | S | — | No workflow holds a token that can write to a forge's registry: snapshots resolved with a read-only token, and a Build on a forge other than GitHub without `MAVEN_SNAPSHOTS_URL` fails at once. |

## Backlog

| # | Size | Blocked by | What it covers |
|---|---|---|---|
| <a id="sl35"></a>[SL35](SL35-The-Sluice-Agent-Works-In-A-Porelo-Task.md) | S | — | The sluice's agent works in a Porelo task, the first to move: the dry run on this laptop done, then a trial on the host with its forge and the way back; its image from `porelo.yml` unchanged, the room under `Agent Sluice`, the reading from outside, a fix amended. Every remaining step needs the host. Back in the backlog until a task on its host fetches the snapshots it builds against from the host's forge. |

## Analyzed

| # | Size | Blocked by | Open questions | What it covers |
|---|---|---|---|---|
| <a id="sl24"></a>[SL24](SL24-A-Code-Comment-The-Filters-Side.md) | L | — | 0 | A review comment as SARIF 2.1.0 passes only in the closed profile: anchors checked by shape, prose judged as prose, one bounded fix judged by detectors measured on code, any other field or claimed authority refused both ways. |

## Idea

| # | Size | Blocked by | What it covers |
|---|---|---|---|
| <a id="sl34"></a>[SL34](SL34-A-Dependabot-Pull-Request-Gets-The-Variables.md) | — | — | A Dependabot pull request's *Workflow check* receives the repository variables, so Maven reads snapshots from a named URL. |

## Estimated

| # | Size | Blocked by | What it covers |
|---|---|---|---|
| <a id="sl04"></a>[SL04](SL04-An-External-Detector-Measured.md) | L | — | Somebody else's detector catalog behind the interface, measured before anything is adopted. |
| <a id="sl23"></a>[SL23](SL23-The-Narrowed-Message-Schema-The-Filters-Side.md) | M | — | Porelo's narrowed message schema enforced in the envelope: four `metadata` keys, five kinds with their fixed data parts, one text part, one extension URI. |
| <a id="sl08"></a>[SL08](SL08-Refuse-A-Payload-Spread-Over-Several-Messages.md) | XL | — | A payload cut into pieces and sent over several messages, caught by correlating against what the task already sent. |

## Finished

| # | Title | What was built |
|---|---|---|
| <a id="sl33"></a>[SL33](README.md#sl33) | The Workflows Run On The Local Forge As On GitHub | The Build runs unchanged on the local Forgejo and on GitHub, switched only by variables: missing package tools installed, Publish switched by `PORELO_PUBLISH`, snapshots read from `MAVEN_SNAPSHOTS_URL`. |
| <a id="sl32"></a>[SL32](README.md#sl32) | Refuse personal and financial data | Payment cards, IBANs and national ids are refused, e-mail addresses, phone numbers, IP addresses and dates of birth reported suspicious, cards and IBANs told by their checksums, matched values redacted in answers, and false positives under 1 % on clean English. |
| <a id="sl07"></a>[SL07](README.md#sl07) | Refuse credentials | A catalog of pattern, validation and context catches ten credential categories in text and metadata, with matched values redacted in answers, rules switchable, and false positives under 1 % on clean English. |
| <a id="sl12"></a>[SL12](README.md#sl12) | Refuse a description that stopped being true | A documents test refuses a mismatch between the modules named in `AGENTS.md`, the root pom's reactor, and the directories on disk, with each failure naming both sides and each module having its page in `doc/`. |
| <a id="sl31"></a>[SL31](README.md#sl31) | Publish without the JFrog action | The Publish job reaches Artifactory with `curl` and the token on standard input, removing `jfrog/setup-jfrog-cli` so no third-party action handles the publishing token. |
| <a id="sl30"></a>[SL30](README.md#sl30) | Old runs deleted on a named runner | *Delete old runs* and the *Shared rules check* run on `ubuntu-26.04` instead of `ubuntu-latest`, so runner version changes require an explicit commit here. |
| <a id="sl29"></a>[SL29](README.md#sl29) | Built in a Porelo task | `porelo.yml` names the tools of the whole local check, and that check, native images and both packages included, ran green inside a Porelo task built from it. |
| <a id="sl28"></a>[SL28](README.md#sl28) | Modes that cannot go together are refused | A command line asking for two of the four modes, or for `--file` or `--dir` without its mode, runs nothing and exits 2 naming both. |
| <a id="sl25"></a>[SL25](README.md#sl25) | The code changed since the skills reading | The code changed since 2026-09-27 read against the seven skills and `shellcheck`, its sixteen findings fixed (each that guards a harm with a test seen red), published as `0.5.1~snapshot.51`. |
| <a id="sl27"></a>[SL27](README.md#sl27) | Reformatted to the shared style | Spotless with the shared Java, YAML and shell formats, one reformatting commit that `git blame` skips, the shared `.editorconfig`, and checkouts without a kept token. |
| <a id="sl26"></a>[SL26](README.md#sl26) | The rename to Porelo here | The filter is published as `org.fuin.porelo` 0.5.0 and installed where Porelo looks for it, accepts only signatures in the namespace `porelo-message`, and a documents test refuses the old name in any tracked file. |

Nothing is in Rework, Implementing, Accepted or Rejected.

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

This is a program that reads and writes directories. **Porelo** is the other half - the mailbox mounted into a task, who feeds this filter, which
transport a peer uses, and what may be in a message in the first place.
