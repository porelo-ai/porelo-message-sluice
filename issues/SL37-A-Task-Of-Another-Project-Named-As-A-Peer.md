# SL37 — A Task Of Another Project Named As A Peer

**Status:** Review.
**Size:** S - one pattern in `EnvelopeCheck`, its tests and one line of `doc/filter.md`, but a contract Porelo reads.

**What must be true.** A message whose `metadata.to` or `metadata.from` names a task of another project by its
principal, `task:<project>:<task>`, passes the envelope check, while a `:` anywhere else in a peer name is still
refused as `ENVELOPE/PEER_NAME_MALFORMED`.

## Plan

- `EnvelopeCheck.PEER` today takes `[A-Za-z0-9][A-Za-z0-9._@-]*` and refuses every `:`. Porelo names a task of
  another project by its certificate's principal and checks a peer name with this whole-string pattern, which the
  filter takes character for character:

      [a-zA-Z0-9][a-zA-Z0-9_.@-]*|task:(?=[a-z0-9:-]{3,57}\z)[a-z0-9][a-z0-9-]{0,62}:(?!(?:people|person|[0-9]+)\z|porelo-)[a-z0-9]([a-z0-9-]*[a-z0-9])?

  The first alternative is today's rule; the second takes exactly the tasks a start can make: `default` is a
  project name like any other, `<project>:<task>` is 3 to 57 characters, so the container name
  `porelo-<project>-<task>` fits in 64, and a task named `people`, `person` or only digits, or beginning `porelo-`
  as a start's container name does, is refused. The length
  limit (`maxIdLength`) stays for both.
- The finding's text and `doc/filter.md`'s row for `ENVELOPE/PEER_NAME_MALFORMED` name the second form.
- The comment on `PEER` says why a peer may be a principal; it no longer names `machine-signers`, which Porelo
  has dropped.
- Red first, in `MessageSluiceTest`: a message to `task:billing:reviewer` and from `task:default:builder` is
  accepted; it fails today. Beside it, refused: `task::x`, `task:Billing:x` (upper case project), `task:p:`,
  `task:p:x:y`, `task:p:-x`, `task:p:x-`, `task:p:people`, `task:p:person`, `task:p:42`, `task:p:porelo-x`, `a:b`,
  `task:p:../x`, and `<project>:<task>` of 58 characters, while one of 57 passes.

## Acceptance

- The accepted message lands in `accepted/`, each refused one in `rejected/` with `ENVELOPE/PEER_NAME_MALFORMED`,
  observed in the files, not in the check's own report.
- Widening the pattern to allow `:` anywhere turns the refused cases red.
