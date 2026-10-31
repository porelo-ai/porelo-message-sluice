# SL37 — A Task Of Another Project Named As A Peer

**Status:** Analyzed.
**Size:** S - one pattern in `EnvelopeCheck`, its tests and one line of `doc/filter.md`, but a contract Porelo reads.

**What must be true.** A message whose `metadata.to` or `metadata.from` names a task of another project by its
principal, `task:<project>:<task>`, passes the envelope check, while a `:` anywhere else in a peer name is still
refused as `ENVELOPE/PEER_NAME_MALFORMED`.

## Plan

- `EnvelopeCheck.PEER` today takes `[A-Za-z0-9][A-Za-z0-9._@-]*` and refuses every `:`. Porelo names a task of
  another project by its certificate's principal: `task:`, a project name (`[a-z0-9][a-z0-9-]{0,62}` or
  `default`), `:`, and a task name as Porelo allows it. The peer rule takes today's names or exactly that form,
  as one pattern of two alternatives; the length limit (`maxIdLength`) stays for both.
- The finding's text and `doc/filter.md`'s row for `ENVELOPE/PEER_NAME_MALFORMED` name the second form.
- The comment on `PEER` says why a peer may be a principal; it no longer names `machine-signers`, which Porelo
  has dropped.
- Red first, in `MessageSluiceTest`: a message to `task:billing:reviewer` and from `task:default:builder` is
  accepted; it fails today. Beside it, refused: `task::x`, `task:Billing:x` (upper case project), `task:p:`,
  `task:p:x:y`, `a:b`, `task:p:../x`, and a principal longer than `maxIdLength`.

## Acceptance

- The accepted message lands in `accepted/`, each refused one in `rejected/` with `ENVELOPE/PEER_NAME_MALFORMED`,
  observed in the files, not in the check's own report.
- Widening the pattern to allow `:` anywhere turns the refused cases red.

## To be checked

- Porelo's exact pattern for a task name and for the whole principal, so both sides take one form; the filter
  matches it character for character.
