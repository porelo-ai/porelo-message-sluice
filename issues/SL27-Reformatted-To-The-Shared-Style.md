# SL27 — Reformatted to the shared style

**Status:** Review. Pushed and published as `porelo-message-sluice-filter` `0.5.1~snapshot.51`.
**Size:** S - one plugin switched on in this repository's poms, one `spotless:apply` over 73 Java
files, two scripts and five YAML files, the shared `.editorconfig`, and the whole workflow on the VM;
no contract another repository reads.

**What must be true.** This repository's Java follows the shared style, and its build refuses a file
that does not: `spotless:check` runs in `validate`, here as in every Maven repository.

From porelo-project [PJ29](https://github.com/porelo-ai/porelo-project/blob/main/issues/README.md#pj29),
which sets the style, the formatter and its version; this is this repository's share of it.

## Why

The Java here is consistent only because each change copied what it saw: four-space indents and lines
up to 120 columns, a length nothing checks. [PJ29](https://github.com/porelo-ai/porelo-project/blob/main/issues/README.md#pj29) gives every repository one style the IDE and the
build both follow, and this repository is reformatted to it once.

## What the code has today

Counted on `main` at `6b54aeb`:

- **73 Java files, 8,963 lines**, 889 of them longer than 100 columns. The most are in
  `MessageSluiceTest` (87), `MessageSluice` (84), `a2a/EnvelopeCheck` (74),
  `detect/EncodingSignatureDetector` (55) and `Settings` (34).
- **No `.editorconfig` and no `.git-blame-ignore-revs`.**
- **No formatter or style check** in any pom.
- **The parent is `porelo-parent` `0.2.0`,** which has no Spotless.

## Plan

1. **The parent:** `pom.xml` moves from `porelo-parent` `0.2.0` to `0.2.1-SNAPSHOT`, once the shared
   part of [PJ29](https://github.com/porelo-ai/porelo-project/blob/main/issues/README.md#pj29) is installed for the hand-over or published. The plugin is switched on in the root pom from the parent's
   plugin management, so `filter/app` inherits it.
2. **Red first:** with the plugin on and nothing reformatted, `./mvnw validate` fails, naming the
   misformatted files.
3. **The reformatting commit:** `spotless:apply` once, and the shared `.editorconfig` from
   porelo-buildtools, which `check-shared` compares. That commit holds nothing else.
   - **The other files go to 4 spaces, never tabs**, as the shared file says. The four YAML files
     under `.github/` and `mkdocs.yml` are indented by 2 today, and move. The poms, `settings.xml` and
     the two scripts already indent by 4. `mvnw` and `mvnw.cmd` stay exactly as the Maven wrapper
     generates them, as the shared file's own section for them says.
   - **The two `bash` scripts**, `install-local.sh` and `buildtools/acceptance.sh`, are formatted by
     `shfmt` through Spotless, pinned in the parent. What `shellcheck` finds in them is
     [SL25](README.md#sl25)'s, not this commit's.
4. **The check:**
   - `git diff -w` on that commit shows only line breaks, Javadoc rewrapped and the order of imports:
     `google-java-format` runs with its defaults, as the operator decided.
   - Its hash goes into `.git-blame-ignore-revs` in the commit after it.
5. **On the VM, on the exact commit:** the whole workflow, that is unit, documents, the 21 fixtures
   against both packages, `LinkageIT` and `PackageVersionIT`. The formatter moves no code, so the
   counts stay as they are.

## Acceptance

- A misformatted Java file fails `validate`, locally and in the Build, naming the file. After
  `spotless:apply` it passes. Seen to fail once here.
- The reformatting commit changes only whitespace, line breaks (Javadoc rewrapped too) and the order
  of imports, and `git blame` skips it.
- `check-shared` compares this repository's `.editorconfig` with the shared one.
- No file in the repository is indented by a tab or by 2 spaces.
- The whole workflow is green on the VM, with the same test counts as before.

## Done so far

- **The parent and the tools** come from the handover of [PJ29](https://github.com/porelo-ai/porelo-project/blob/main/issues/README.md#pj29)'s shared part: porelo-parent `0.2.1-SNAPSHOT` with Spotless
  3.10.3, `google-java-format` 1.36.0 and `shfmt` 3.12.0, and the tools `0.5.1-SNAPSHOT`.
- **Seen red:** `validate` named both scripts, then a misformatted `Settings.java`, and `check-actions`
  refused `build.yml`'s checkout without `persist-credentials: false`.
- **The reformatting commit** changes 66 files. Per Java file, with whitespace removed and the imports
  compared as sets, what is left is the leaders of rewrapped Javadoc and line comments (`*`, `//`).
  The five YAML files load to the same data. Beside the formatting, it holds the shared
  `.editorconfig`, `.gitignore`'s two lines, the Shared Area at `01f9fdbdfaaa9c51`, `check-shared
  AGENTS.md .editorconfig`, `persist-credentials: false` on both checkouts, and `WorkflowLimitsTest`
  reading jobs at 4 spaces and their limits at 8. It went red on the YAML at 4.
- **`.git-blame-ignore-revs`** names it, in the commit after it.

## To be checked

- **Comments that must stay on their line.** `check-citations` reads a `// not-a-citation` marker
  on the line it exempts. Where the formatter wraps such a line, the marker must still be on the
  line that holds the citation. Checked: all three in `SplitPayloadTest` stay on their lines, and
  `check-citations` passes.
- **Long string literals.** Checked: the parent does not reflow them, so a literal past 100 columns
  stays on its line, and `spotless:check` passes.
