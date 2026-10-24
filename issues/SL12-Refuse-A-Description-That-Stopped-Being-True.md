# SL12 — Refuse a description that stopped being true

**Status:** Review.
**Size:** S - one test that reads `AGENTS.md`, the poms and the directories, in this repository's own
build; no contract another repository reads.

**What must be true.** Where a sentence in this repository has a counterpart the build can read, the
build refuses the two disagreeing: the modules a description names are the modules that exist.

## Why

The shared `check-citations` asks whether what a document points to still exists; nothing asks
whether what exists is still described. A description of the modules here once named fewer than the
reactor held, and no pointer check could see it, since everything it named existed.

## The shape

- **The modules named in `AGENTS.md` are the modules in `pom.xml`**, the same set, and the count in
  the sentence matches what it names.
- **Every module has a contract in `doc/`**, and every contract in `doc/` names a module that exists.
- **Three sources, not two**: the prose, the reactor, and the directories on disk that carry a
  `pom.xml`, so a module directory outside the build cannot let prose and reactor agree while both
  are wrong.
- **Out of scope:** a description of behaviour, a rationale, an accepted risk, or anything with no
  counterpart a build can read. That counterpart is the test for adding a fourth item.

## Plan

- **One test, `ModulesDescribedTest`, tagged `documents`,** beside `DocumentTestsTaggedTest` in `filter/app`,
  reading three things from the repository root: the modules the first paragraph of `AGENTS.md`'s *What this
  repository is* names as directories (`` `filter/` ``) and the number it says ("one Maven module"), the
  `<module>` entries of the root `pom.xml`, profiles included, and the directories beside it that carry a
  `pom.xml`.
- **The `doc/` side:** each module named in the prose has its page `doc/<module>.md`. The other way round is
  not held: `decisions.md`, `detectors.md` and `index.md` are pages of the whole, not of a module.
- **Modules at the root only:** the sentence names `filter/`, and `filter`'s own submodules (`app`, `dist-deb`,
  `dist-rpm`, which `doc/filter.md` covers as one program) are `check-readmes`' to hold, which already reads every
  directory with a `pom.xml` and its links.
- **Next to `check-readmes`,** which already holds every directory with a `pom.xml` to a `README.md`
  that links its submodules; this test adds the prose and `doc/`, which that check does not read. It
  stays this repository's own test: the shared check reads no prose, and takes a convention only
  once a second repository holds it.
- **Red first:** today's prose names `filter/` and the reactor holds `filter` - green; with a module
  added to the reactor only, the test must name both sides.
- **Seen red**, each alone and restored: a module added to the reactor (with its directory), the module taken
  out of the sentence, the sentence saying "two", a directory with a `pom.xml` outside the reactor, and
  `doc/filter.md` gone. Each failure names both sides, e.g. *"`AGENTS.md` names [filter]; `pom.xml` has
  [filter, other]"*.
- **Read against the skills** (graal 2026.910.3; java-code-review, test-quality, security-audit,
  concurrency-review, clean-code and solid-principles 2026.906.3): no finding names a harm. Declined, each for
  reading only: test-quality's one concept per test (each failure names both sides, and one red is what the
  acceptance asks for), clean-code's method over twenty lines, a stream collected in two steps. The test reads the
  working tree, so an untracked directory with a `pom.xml` beside the root turns it red: that fails loud, never
  green.

## Acceptance

- Seen to fail: remove a module from the prose, or add one to the reactor, and exactly one test goes
  red naming both sides, e.g. *"`AGENTS.md` names filter; `pom.xml` has filter, other"*.
- The walk asserts it found modules and prose items, so an empty list cannot pass or fail wholesale.
- It runs in this repository's own build, against its own `pom.xml`.
