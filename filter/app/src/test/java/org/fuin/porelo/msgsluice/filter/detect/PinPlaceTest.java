package org.fuin.porelo.msgsluice.filter.detect;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.stream.Stream;
import org.fuin.porelo.msgsluice.filter.decision.Finding;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * A full commit id or digest written where a pin stands - after {@code @} in a {@code uses:} line,
 * after {@code sha256:} and in an image reference's {@code @sha256:} - passes in code and in prose,
 * since pinning is what the repositories' own rules ask for. Harm guarded on the other side: the
 * exemption widening into a channel, so the same string anywhere else, a run of another length and
 * a second run after the pin stay refused, and a refusal never repeats the run.
 */
class PinPlaceTest {

    private static final String COMMIT_ID = MaskedTest.hex(20, 35);

    private static final String DIGEST = MaskedTest.hex(32, 64);

    /** Each place, written with {@code %s} for the hash, as code and prose write it. */
    private static final List<String> PLACES =
            List.of(
                    "- uses: actions/checkout@%s # v7.0.1",
                    "    uses: actions/cache/restore@%s",
                    "uses: \"actions/checkout@%s\"",
                    "sha256: %s",
                    "  sha256: \"%s\"",
                    "sha256: '%s'",
                    "sha256:%s",
                    "image: codeberg.org/forgejo/forgejo:17.0.0-rootless@sha256:%s",
                    "FROM docker.io/library/eclipse-temurin@sha256:%s AS build");

    /** The same places as a sentence in prose puts them, in backticks and parentheses. */
    private static final List<String> PROSE_PLACES =
            List.of(
                    "Pin the step as `uses: actions/checkout@%s` and keep the version beside it.",
                    "The lockfile now says `sha256: \"%s\"` for the package.",
                    "The image is pinned (forgejo@sha256:%s) since the last update.");

    static Stream<String> pins() {
        return Stream.of(COMMIT_ID, DIGEST).flatMap(h -> PLACES.stream().map(p -> p.formatted(h)));
    }

    static Stream<String> prosePins() {
        return Stream.of(COMMIT_ID, DIGEST)
                .flatMap(
                        h ->
                                Stream.concat(PLACES.stream(), PROSE_PLACES.stream())
                                        .map(p -> p.formatted(h)));
    }

    /** The same hashes where no pin stands, and runs at a pin place that are no full hash. */
    static Stream<String> nearPins() {
        return Stream.of(
                // other lengths at a pin place
                "uses: actions/checkout@" + COMMIT_ID.substring(1),
                "uses: actions/checkout@" + COMMIT_ID + "a",
                "sha256: " + DIGEST.substring(1),
                "sha256: " + DIGEST + "a",
                "sha256: " + DIGEST + COMMIT_ID,
                // a second run after the pin
                "sha256: " + DIGEST + " " + DIGEST,
                "uses: actions/checkout@" + COMMIT_ID + " " + COMMIT_ID,
                "sha256: " + DIGEST + "#" + DIGEST,
                "sha256: " + DIGEST + ")" + DIGEST,
                "uses: actions/checkout@" + COMMIT_ID + "`" + COMMIT_ID,
                // the same hash where no pin stands
                "ref: actions/checkout@" + COMMIT_ID,
                "git checkout " + COMMIT_ID,
                "echo " + DIGEST,
                "MUSL_SHA256=" + DIGEST,
                "musl_sha256: " + DIGEST,
                "musl-sha256: " + DIGEST,
                "x.sha256: " + DIGEST,
                "/sha256:" + DIGEST,
                "muslésha256: " + DIGEST,
                "x.uses: actions/checkout@" + COMMIT_ID,
                "éuses: actions/checkout@" + COMMIT_ID,
                "checksum: " + DIGEST,
                "fake_uses: actions/checkout@" + COMMIT_ID,
                "uses: actions/checkout@v7 " + COMMIT_ID,
                "sha256: " + DIGEST.toUpperCase(java.util.Locale.ROOT));
    }

    @ParameterizedTest
    @MethodSource("pins")
    void aFullHashAtAPinPlacePassesInCode(final String line) {
        assertThat(CodeRatesTest.judge(line).accepted()).as("code: %s", line).isTrue();
    }

    @ParameterizedTest
    @MethodSource("prosePins")
    void aFullHashAtAPinPlacePassesInProse(final String line) {
        assertThat(EncodedPayloadRatesTest.judge(line).accepted()).as("prose: %s", line).isTrue();
    }

    /** A workflow pins every step; however many pins it holds, they pass. */
    @Test
    void aWorkflowWithManyPinsPasses() {
        final StringBuilder workflow = new StringBuilder("steps:\n");
        for (int i = 0; i < 40; i++) {
            workflow.append("  - uses: actions/step-")
                    .append(i)
                    .append('@')
                    .append(MaskedTest.hex(20, 1000 + i))
                    .append(" # v")
                    .append(i)
                    .append(".0.0\n");
        }

        assertThat(CodeRatesTest.judge(workflow.toString()).accepted())
                .as("a workflow of 40 pinned steps")
                .isTrue();
    }

    /** A text of nothing but pin prefixes that never reach a hash is read in linear time. */
    @Test
    void aLongRunOfPinPrefixesIsReadQuickly() {
        final String text = "uses:".repeat(64 * 1024 / 5);
        final long start = System.nanoTime();

        CodeRatesTest.judge(text);
        EncodedPayloadRatesTest.judge(text);

        assertThat((System.nanoTime() - start) / 1_000_000)
                .as("milliseconds to judge 64 KB of pin prefixes as code and prose")
                .isLessThan(5_000);
    }

    /** 64 KB of every pin place, each line with a hash of its own, so every prefix reaches one. */
    @Test
    void sixtyFourKilobytesOfRealPinsPassQuickly() {
        final StringBuilder text = new StringBuilder();
        for (int i = 0; text.length() < 64 * 1024; i++) {
            final String hash = MaskedTest.hex(i % 2 == 0 ? 20 : 32, 2000 + i);
            text.append(PLACES.get(i % PLACES.size()).formatted(hash)).append('\n');
        }

        assertJudgedQuickly(text.toString(), true);
    }

    /** 64 KB of pins one after the other on one line, so no line break ends a path. */
    @Test
    void sixtyFourKilobytesOfPinsOnOneLinePassQuickly() {
        final StringBuilder text = new StringBuilder();
        for (int i = 0; text.length() < 64 * 1024; i++) {
            text.append("uses: actions/step@").append(MaskedTest.hex(20, 3000 + i)).append(' ');
        }

        assertJudgedQuickly(text.toString(), true);
    }

    /**
     * 64 KB of the longest path a pin may take, each ending in a hash one character short, so every
     * prefix is read to its end and fails there.
     */
    @Test
    void sixtyFourKilobytesOfNearPinsAreRefusedQuickly() {
        final StringBuilder text = new StringBuilder();
        for (int i = 0; text.length() < 64 * 1024; i++) {
            text.append("uses: ")
                    .append("a".repeat(255))
                    .append('@')
                    .append(MaskedTest.hex(20, 4000 + i), 1, 40)
                    .append('\n');
        }

        assertJudgedQuickly(text.toString(), false);
    }

    private static void assertJudgedQuickly(final String text, final boolean accepted) {
        final long start = System.nanoTime();

        final boolean code = CodeRatesTest.judge(text).accepted();
        final boolean prose = EncodedPayloadRatesTest.judge(text).accepted();

        assertThat((System.nanoTime() - start) / 1_000_000)
                .as("milliseconds to judge %d characters as code and prose", text.length())
                .isLessThan(5_000);
        assertThat(code).as("accepted as code").isEqualTo(accepted);
        assertThat(prose).as("accepted as prose").isEqualTo(accepted);
    }

    @ParameterizedTest
    @MethodSource("nearPins")
    void aHashWhereNoPinStandsIsRefusedInCodeAndProse(final String line) {
        assertThat(CodeRatesTest.judge(line).accepted()).as("code: %s", line).isFalse();
        assertThat(EncodedPayloadRatesTest.judge(line).accepted()).as("prose: %s", line).isFalse();
    }

    @ParameterizedTest
    @MethodSource("nearPins")
    void aRefusedNearPinIsNotRepeated(final String line) {
        final List<Finding> findings =
                Stream.concat(
                                CodeRatesTest.RUNNER
                                        .run(List.of(TextUnderCheck.code("t", 0, line)))
                                        .stream(),
                                EncodedPayloadRatesTest.RUNNER
                                        .run(List.of(new TextUnderCheck("t", 0, line)))
                                        .stream())
                        .toList();

        assertThat(findings)
                .as("findings for %s", line)
                .isNotEmpty()
                .allSatisfy(
                        f -> {
                            assertThat(String.valueOf(f.redactedExcerpt()))
                                    .as("excerpt")
                                    .doesNotContainPattern("(?i)[0-9a-f]{8}");
                            assertThat(f.reason())
                                    .as("reason")
                                    .doesNotContainPattern("(?i)[0-9a-f]{8}");
                        });
    }
}
