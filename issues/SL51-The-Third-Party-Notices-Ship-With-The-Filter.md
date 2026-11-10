# SL51 — The Third-Party Notices Ship With The Filter

```json
{
  "$schema": "issue-1.json",
  "id": "SL51",
  "title": "The Third-Party Notices Ship With The Filter",
  "state": "Idea",
  "size": "S",
  "priority": "Medium",
  "rank": 1,
  "blockedBy": [],
  "covers": "The .deb and the .rpm carry one THIRD-PARTY-NOTICES file with the full license and notice texts of everything compiled into the native filter, made by the shared generator."
}
```

**Why this size.** One module's build gains two steps, both packages one file each, and the packaged-binary step one
check; no contract another repository reads.

**What must be true.** Whoever installs the filter's `.deb` or `.rpm` finds the full license and notice texts of
every third-party piece compiled into the native executable in one file, `THIRD-PARTY-NOTICES`, beside the filter's
own license, as the licenses of those pieces require.

## Why

The native executable compiles in Jackson (`jackson-databind`, `jackson-core`, `jackson-annotations`), the
FastDoubleParser that `jackson-core` embeds with its own license and notice, JSpecify, and the GraalVM runtime. The
packages today carry only the filter's own `LICENSE` and a `debian/copyright` that names nothing else; the jars'
`META-INF/LICENSE`, `NOTICE` and embedded licenses do not reach an installed system.

## The shape

- The file is made at build time by the shared generator in the Porelo build tools, which collects every compiled-in
  jar's `LICENSE` and `NOTICE` and the licenses they embed; this repository keeps no list of its own.
- The `.deb` ships it as `/usr/share/doc/porelo-message-sluice-filter/THIRD-PARTY-NOTICES`, and `dist-common/copyright`
  names it.
- The `.rpm` ships it as `%license`, under `/usr/share/licenses/porelo-message-sluice-filter/`.
- Nothing is built before the generator is published.

## Plan

Read in today's code: the native executable is built in `filter/app` (profile `native`), its CycloneDX bill
`filter/app/target/bom.json` in profile `dist`, and the packages in `filter/dist-deb` and `filter/dist-rpm`, which
already map `bom.json`, `LICENSE` and `dist-common/copyright`.

- `filter/app/pom.xml`, profile `dist`, after `package` and the bill: `maven-dependency-plugin:build-classpath`
  with `includeScope=runtime` into `target/classpath.txt`, then `porelo-release third-party-notices
  target/classpath.txt target/bom.json "$JAVA_HOME" target/THIRD-PARTY-NOTICES --own
  'org\.fuin\.porelo:porelo-message-sluice.*'`, run by `exec-maven-plugin` with `porelo-release` as its pinned
  dependency, so the JDK is the pinned GraalVM the image is built with.
- `filter/dist-deb/pom.xml`: the file as `/usr/share/doc/porelo-message-sluice-filter/THIRD-PARTY-NOTICES`;
  `dist-common/copyright` names it.
- `filter/dist-rpm/pom.xml`: the file under `/usr/share/licenses/porelo-message-sluice-filter/`, marked `%license`.
- Red first: the packaged-binary step of `build.yml` (run here verbatim) fails when either unpacked package lacks the
  file or the file lacks jackson-core's FastDoubleParser license; it goes red before the mapping exists.
- The generator refuses, and so fails the build, when a runtime jar has no bill entry or no license: that is its own
  tested behavior, not repeated here. Today's bill names Apache-2.0 for all four runtime jars (jackson-databind,
  -core, -annotations, jspecify), so it passes; the GraalVM runtime's terms come from the JDK's `legal/` directory.

Test levels: no unit test (nothing in the filter's logic changes); the packaged-binary step on the host and in the
Build; no VM, since what is checked is the packages' content, not an install.

## Acceptance

- Both packages, unpacked, hold the file, and it holds the full texts of Jackson's license and notice,
  FastDoubleParser's license and notice, JSpecify's license and the GraalVM runtime's terms.
- The packaged-binary checks fail when the file is missing from either package.
