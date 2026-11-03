# SL39 — An Organization Proved Against Real GitHub

**Status:** Idea.

**What must be true.** A person can make and read an organization kept at a `git@github.com:` address on the real
forge: `credentials trust-host github.com`, `org create`, `setup` on a second machine that finds the person's card by
itself, and `project open` making the project's repository, private, with a signed first commit, after which `org`
reads with nothing stale and nothing refused. So far each of these has run only against a local forge or a stand-in.
The short test takes a person about ten minutes, needs no host and no interface, runs on an unpacked package in folders
of its own, and removes everything it made, after a failure too; it takes place after the release that follows the
current one.

## To be checked

- The test's scripts name a package version and its digest comes from the signed index at run time: they must name the
  package current when this is taken up.
- The test declares its token by an environment variable, so a token declared by a file stays unproved at this address.
- The rights the token needs are as the forge documents them, not measured; the first run measures them.
