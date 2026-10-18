# Contributing

Changes here follow the project's guide, [Contributing to Porelo](https://porelo-ai.github.io/porelo-project/contributing/):
how a change starts as a GitHub issue, what a commit carries, and what a change must satisfy. The
rules are kept there and in the shared block of [`AGENTS.md`](AGENTS.md), not here.

## In this repository

**How it is built and tested** is in [`build.md`](build.md): `./mvnw -B -s settings.xml verify`
for the code and its tests, and `-Pnative,dist` with a GraalVM 25 as `JAVA_HOME` for the native
executable and its `.deb` and `.rpm`. What is particular here:

- **The code style is checked on `validate`**, by Spotless from `porelo-parent`: Java and YAML, and
  the two shell scripts through `shfmt`, which must be on the `PATH` at the version the Build pins.
  `./mvnw -s settings.xml spotless:apply` puts a file right.
- **The documents tests** run with `-Pdocuments test`; they read the documentation, not the code.
- **The filter fails closed and holds no language model.** A change to what it lets through comes
  with the fixtures that show it, in [`filter/app/src/test/resources/fixtures/`](filter/app/src/test/resources/fixtures/README.md),
  and the reasons behind its rules are in [`doc/decisions.md`](doc/decisions.md).
- **Its interface is read by other repositories**: the mailbox's directories, the message format,
  the feedback files and the command line Porelo calls. A change to any of them is agreed in
  [porelo](https://github.com/porelo-ai/porelo) first.
