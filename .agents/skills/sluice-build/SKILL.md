---
name: sluice-build
description: porelo-message-sluice's build and checks: the files that are not Java, running the workflow's steps here, the documents tests and checks before a hand-over, the packaged-binary fixture step. Use before a hand-over, a test run, or changing a file that is not Java.
---

# sluice-build

## Work in this repository

- **Not Java, each because it runs where there is no Java:** `install-local.sh` copies the native
  filter where Porelo looks for it; `buildtools/acceptance.sh` checks the installed package on a
  machine that has nothing of this repository but the package.
- **The workflow's steps that run no Maven can be run here verbatim**, with `RUNNER_TEMP` and
  `GITHUB_WORKSPACE` set, against the packages the native build wrote.
- **The tests tagged `documents` run alone with `./mvnw -B -s settings.xml -Pdocuments test`**, and
  `DocumentTestsTaggedTest` fails when a test reads a document without the tag; none reads one today.
- **The documents are checked on the host before every hand-over**, on its exact commit, and by the
  `shared-rules` workflow once a day and on a pull request, while the build skips a change to
  documents alone: `check-shared`, `check-citations`, `check-doc-site`,
  `check-readmes`, `check-snapshot-filter` and the tests tagged `documents`. `citations-exempt.txt` names the measured
  corpus, whose lines hold the shape of issue numbers on purpose.
- **Run the packaged-binary fixture step from a file extracted verbatim from the workflow.** Never
  embed it in an unquoted heredoc that changes its shell expansions.
