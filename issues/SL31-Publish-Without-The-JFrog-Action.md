# SL31 — Publish without the JFrog action

**Status:** Review.
**Size:** S - two steps of `build.yml`, no code that ships, no changelog entry; its acceptance is the next Publish
on main, read in its log and in the index.

**What must be true.** The Build's Publish job reaches Artifactory with `curl` and the token in its environment, as
the agent repositories' Publish does, instead of through `jfrog/setup-jfrog-cli`, so one third-party action less
handles the publishing token.

## Plan

- **Read in `build.yml` on `538a37f`:** *Set up the JFrog CLI* (`jfrog/setup-jfrog-cli` v5.3.0) gets
  `JF_ACCESS_TOKEN`, and *Publish the packages* then calls `jf rt upload`: each `.deb` into
  `porelo-dist-deb/pool/main/p/<package>/` with the properties `deb.distribution`, `deb.component` and
  `deb.architecture`, and each `.rpm` into `porelo-dist-rpm/<channel>/`. *The packages are indexed, not merely
  stored* already reads the index anonymously with `curl -sfL` and stays as it is.
- **What replaces it:** *Set up the JFrog CLI* goes. *Publish the packages* takes `JF_ACCESS_TOKEN` in its own
  `env` and hands it to each `curl -fsS -K - -T <file>` on standard input, from the shell's own `printf`: the
  shared rule says a secret never touches a filesystem in CI, so it goes into no file and onto no command line.
  Its SHA-1 and SHA-256 travel as `X-Checksum-*` headers, so
  Artifactory refuses bytes that do not match, and the `.deb`'s properties are matrix parameters on the path. That
  is the shape of the agent repositories' `.github/scripts/artifactory.sh`, written into the step itself, with no
  helper file: a sixth copy would break the rule that no repository keeps a copy of a helper another one has. One
  helper for all of them is porelo-buildtools' to publish.
- **The release check before it** ("is published already") reads the index into a file and stops when it cannot
  be read, as the agent repositories' does, instead of `|| true`, which reads an unreadable index as "not
  published".
- **No test:** a workflow step is checked by the run it starts. `check-actions` stays green with one action less.
  Before the push, the step ran alone with `curl` and `dpkg-deb` stood in for: the addresses, properties and
  checksums it sends, the configuration on standard input, the token in no file and on no command line, and each of its
  three refusals (no token, a release already published, an index that cannot be read). `shellcheck` finds
  nothing.

## Acceptance

- The first Publish on main after the push uploads both packages, and *The packages are indexed* finds the
  exact files; read in the run's log. A push of `.github/` alone builds nothing, so that is the first push with
  content after it.
- The log shows no token, and `jfrog/setup-jfrog-cli` is in no workflow (`git grep`).
