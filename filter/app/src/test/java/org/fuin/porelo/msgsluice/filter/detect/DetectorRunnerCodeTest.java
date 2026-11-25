package org.fuin.porelo.msgsluice.filter.detect;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import org.fuin.porelo.msgsluice.filter.Settings;
import org.fuin.porelo.msgsluice.filter.decision.Finding;
import org.fuin.porelo.msgsluice.filter.decision.Redactor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * A text of source code is judged by the code detectors alone: the checks that ask whether a text
 * reads like English would refuse real code, and the credential and encoded-data checks must still
 * see it, or code would be a way around them.
 */
class DetectorRunnerCodeTest {

    /** Remembers every text it was shown and reports nothing. */
    private static final class Recording implements ContentDetector {

        final List<TextUnderCheck> seen = new ArrayList<>();

        @Override
        public String id() {
            return "test/recording@1";
        }

        @Override
        public Set<String> ruleIds() {
            return Set.of("TEST/NONE");
        }

        @Override
        public Set<String> categories() {
            return Set.of("TEST");
        }

        @Override
        public List<DetectorFinding> inspect(final TextUnderCheck text) {
            seen.add(text);
            return List.of();
        }
    }

    private static List<String> ruleIds(final List<Finding> findings) {
        return findings.stream().map(Finding::ruleId).toList();
    }

    @Test
    void aCodeTextReachesOnlyTheCodeDetectorsAndKeepsItsKindThroughNormalization() {
        final Recording prose = new Recording();
        final Recording code = new Recording();
        final DetectorRunner runner =
                new DetectorRunner(
                        List.of(prose), List.of(code), new Redactor(Redactor.Mode.PARTIAL));

        runner.run(
                List.of(
                        TextUnderCheck.code("parts[0].text", 0, "int a = 1;\r\n"),
                        new TextUnderCheck("parts[1].text", 1, "Hello there.")));

        assertThat(code.seen)
                .as("the code detectors see the code text, normalized, and still as code")
                .singleElement()
                .satisfies(
                        t -> {
                            assertThat(t.kind()).as("kind").isEqualTo(TextKind.CODE);
                            assertThat(t.text()).as("normalized text").isEqualTo("int a = 1;\n");
                        });
        assertThat(prose.seen)
                .as("the prose detectors see only the prose text")
                .extracting(TextUnderCheck::location)
                .containsExactly("parts[1].text");
    }

    @Test
    void theDefaultCodeDetectorsLeaveOutTokenShapeAndDistribution() throws Exception {
        final Settings settings = Settings.load(null, Map.of("mail", "/m"));

        assertThat(settings.codeDetectors())
                .as("the code detectors the defaults build")
                .extracting(d -> d.getClass().getSimpleName())
                .containsExactly(
                        "LongTokenDetector",
                        "EntropyDetector",
                        "EncodingSignatureDetector",
                        "CredentialDetector",
                        "PersonalDataDetector");
    }

    @Test
    void aTokenTheShapeCheckReportsInProseIsNotReportedInCode() {
        final String text = "final int id = X9Y8Z7W6V5Q;";

        assertThat(ruleIds(CodeRatesTest.RUNNER.run(List.of(new TextUnderCheck("t", 0, text)))))
                .as("the same token in prose")
                .contains(TokenShapeDetector.DIGIT_UPPER);
        assertThat(ruleIds(CodeRatesTest.RUNNER.run(List.of(TextUnderCheck.code("t", 0, text)))))
                .as("the token in code")
                .doesNotContain(TokenShapeDetector.DIGIT_UPPER);
    }

    @Test
    void aCredentialInCodeIsFoundAndNotRepeated() {
        final String token = "gh" + "p_" + CredentialsTest.random(36);
        final String code = "final String token = \"" + token + "\";\nclient.login(token);";

        final List<Finding> findings =
                CodeRatesTest.RUNNER.run(List.of(TextUnderCheck.code("parts[0].text", 0, code)));

        assertThat(ruleIds(findings)).as("rules found").contains("CREDENTIAL/VCS_TOKEN");
        assertThat(CodeRatesTest.MODEL.decide(findings, true).accepted())
                .as("a message carrying it is accepted")
                .isFalse();
        assertThat(findings)
                .as("no finding's excerpt or reason repeats the token")
                .allSatisfy(
                        f -> {
                            assertThat(String.valueOf(f.redactedExcerpt()))
                                    .as("excerpt")
                                    .doesNotContain(token.substring(4, 16));
                            assertThat(f.reason())
                                    .as("reason")
                                    .doesNotContain(token.substring(4, 16));
                        });
    }

    /** A payload's bytes written as a decimal array are found, and never shown in a finding. */
    @Test
    void sixteenBytesInDecimalInCodeAreRefusedAndNotRepeated() {
        final String values =
                "104, 117, 110, 116, 101, 114, 50, 45, 115, 101, 99, 114, 101, 116, 33, 33";
        final String code = "final int[] data = {" + values + "};";

        final List<Finding> findings =
                CodeRatesTest.RUNNER.run(List.of(TextUnderCheck.code("parts[0].text", 0, code)));

        assertThat(ruleIds(findings))
                .as("rules found")
                .contains(EncodingSignatureDetector.DECIMAL_BYTES);
        assertThat(findings)
                .as("no finding's excerpt or reason repeats the values")
                .allSatisfy(
                        f -> {
                            assertThat(String.valueOf(f.redactedExcerpt()))
                                    .as("excerpt")
                                    .doesNotContainPattern("\\d+\\s*,\\s*\\d+");
                            assertThat(f.reason())
                                    .as("reason")
                                    .doesNotContainPattern("\\d+\\s*,\\s*\\d+");
                        });
    }

    /** Bytes in decimal are found also cast, negative and over lines, as Java writes them. */
    @Test
    void castNegativeAndWrappedDecimalBytesAreRefused() {
        final StringBuilder values = new StringBuilder();
        for (int i = 0; i < 16; i++) {
            values.append(i == 0 ? "" : i % 4 == 0 ? ",\n        " : ", ")
                    .append("(byte) ")
                    .append(i % 2 == 0 ? -100 - i : 200 + i);
        }
        final String code = "final byte[] data = {\n        " + values + "\n};";

        assertThat(ruleIds(CodeRatesTest.RUNNER.run(List.of(TextUnderCheck.code("t", 0, code)))))
                .as("rules found in %s", code)
                .contains(EncodingSignatureDetector.DECIMAL_BYTES);
    }

    /** Sixteen bytes in decimal are found before a trailing comma or a value no byte holds. */
    @ParameterizedTest
    @ValueSource(strings = {",", ", 256", ", x", ", 0x1f", ", 1.5", ",\n}", "L, 70000L"})
    void decimalBytesAreFoundWhateverFollowsThem(final String tail) {
        final String code =
                "final long[] data = {104, 117, 110, 116, 101, 114, 50, 45, 115, 101, 99, 114, 101,"
                        + " 116, 33, -128"
                        + tail
                        + "};";

        assertThat(ruleIds(CodeRatesTest.RUNNER.run(List.of(TextUnderCheck.code("t", 0, code)))))
                .as("rules found in %s", code)
                .contains(EncodingSignatureDetector.DECIMAL_BYTES);
    }

    /** A list of 64 KB of small numbers ending in something else is read in linear time. */
    @Test
    void aLongListOfSmallNumbersIsReadQuickly() {
        final String code = "int[] a = {" + "1, ".repeat(64 * 1024 / 3) + "x};";
        final long start = System.nanoTime();

        final List<Finding> findings =
                CodeRatesTest.RUNNER.run(List.of(TextUnderCheck.code("t", 0, code)));

        assertThat((System.nanoTime() - start) / 1_000_000)
                .as("milliseconds to judge 64 KB of numbers")
                .isLessThan(5_000);
        assertThat(ruleIds(findings))
                .as("the list is found as bytes")
                .contains(EncodingSignatureDetector.DECIMAL_BYTES);
    }

    /**
     * Fifteen values, a value a byte cannot hold, and the same list in prose are no decimal bytes.
     */
    @Test
    void fewerValuesALargerValueOrProseAreNoDecimalBytes() {
        final String fifteen =
                "final int[] a = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15};";
        final String large =
                "final int[] a = {1, 2, 3, 4, 5, 6, 7, 8, -129, 10, 11, 12, 13, 14, 15, 16, 17};";
        final String sixteen = "1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16";

        assertThat(ruleIds(CodeRatesTest.RUNNER.run(List.of(TextUnderCheck.code("t", 0, fifteen)))))
                .as("fifteen values")
                .doesNotContain(EncodingSignatureDetector.DECIMAL_BYTES);
        assertThat(ruleIds(CodeRatesTest.RUNNER.run(List.of(TextUnderCheck.code("t", 0, large)))))
                .as("-129 splits the list into runs of 8 and 8")
                .doesNotContain(EncodingSignatureDetector.DECIMAL_BYTES);
        assertThat(
                        ruleIds(
                                CodeRatesTest.RUNNER.run(
                                        List.of(
                                                new TextUnderCheck(
                                                        "t", 0, "See lines " + sixteen + ".")))))
                .as("line numbers in prose")
                .doesNotContain(EncodingSignatureDetector.DECIMAL_BYTES);
    }

    @Test
    void thirtyTwoCharactersOfBase64InAJavaStringLiteralAreRefused() {
        final String blob =
                Payloads.cut(
                        java.util.Base64.getEncoder()
                                .encodeToString(Payloads.bytes(new Random(7), 32)),
                        32);
        final String code = "final String data = \"" + blob + "\";";

        final List<Finding> findings =
                CodeRatesTest.RUNNER.run(List.of(TextUnderCheck.code("parts[0].text", 0, code)));

        assertThat(ruleIds(findings))
                .as("rules found in %s", code)
                .contains(EncodingSignatureDetector.BASE64);
        assertThat(CodeRatesTest.MODEL.decide(findings, true).accepted())
                .as("a message carrying it is accepted")
                .isFalse();
    }
}
