# SL51 — The Third-Party Notices Ship With The Filter

```json
{
  "$schema": "issue-1.json",
  "id": "SL51",
  "title": "The Third-Party Notices Ship With The Filter",
  "state": "Idea",
  "size": "-",
  "priority": "Medium",
  "rank": 1,
  "blockedBy": [],
  "covers": "The .deb and the .rpm carry one THIRD-PARTY-NOTICES file with the full license and notice texts of everything compiled into the native filter, made by the shared generator."
}
```

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

## Acceptance

- Both packages, unpacked, hold the file, and it holds the full texts of Jackson's license and notice,
  FastDoubleParser's license and notice, JSpecify's license and the GraalVM runtime's terms.
- The packaged-binary checks fail when the file is missing from either package.

## To be checked

- Whether the GraalVM runtime's terms come from the generator or must be supplied beside it.
- Whether JSpecify, compile scope and annotations only, is compiled into the image at all.
