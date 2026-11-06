# SL47 — No Message Is Lost Or Filed Twice

**Status:** Analyzed.
**Size:** M - file handling across `FileOps` and `MessageSluice`, several of it race paths.

**What must be true.** A message and its signature are never overwritten, filed apart or read past their limit: a free name is never a taken one, a signature that went ahead is not filed beside the wrong message, a file over the size limit is answered, a replaced directory is noticed, and the production path with a stability delay passes a message.

## Plan

PIT left these mutants alive, or found the path not covered:

- `FileOps.freeName` and `taken`: the name split at the dot and the test whether a name is taken survive; filing could
  overwrite a message.
- `FileOps.writeWithSignature`, `moveWithSignature`: the case of a signature that went ahead survives.
- `FileOps.deleteWithSignatureIfUnchanged`: skipping the signature's deletion survives, so a signature stays in
  `incoming/` beside the next message of its name; `deleteIfUnchanged` answering deleted for a changed file survives,
  so `keepAsRead` moves the signature away from a message that stays.
- `FileOps.read`: the size limit's arithmetic survives; `MessageSluice.process`'s answer to a file over the limit is
  not covered.
- `MessageSluice.identity`: returning an empty identity survives, so a directory replaced since the start would not be
  noticed.
- `MessageSluice.keep`: choosing between filing the bytes read and moving the file survives.
- `MessageSluice.stableFiles`: returning nothing after the stability delay survives: every test runs with no delay,
  the production setting has one. `process`'s "changed while being read" and `filedBefore` are not covered.
- `FileOps.move`'s test for a regular file survives; `watch` is not covered.
- Red first: a taken name with a dot and without, a signature written first, a file one byte over the limit, a mailbox
  directory swapped for another between passes, and one pass with a stability delay.

## Acceptance

- Each case named under *Plan* has a test that fails against the mutant PIT left, seen red by PIT or by breaking the
  check by hand, and passes against the code.
- PIT on the classes named, with the `pit` profile, leaves none of the places in *Plan* surviving, or this issue names
  the place and why no input tells it apart.
