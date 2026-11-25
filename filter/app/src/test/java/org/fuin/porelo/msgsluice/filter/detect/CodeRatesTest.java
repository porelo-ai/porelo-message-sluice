package org.fuin.porelo.msgsluice.filter.detect;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Function;
import org.fuin.porelo.msgsluice.filter.ConfigException;
import org.fuin.porelo.msgsluice.filter.Settings;
import org.fuin.porelo.msgsluice.filter.decision.DecisionConfig;
import org.fuin.porelo.msgsluice.filter.decision.DecisionModel;
import org.fuin.porelo.msgsluice.filter.decision.Finding;
import org.fuin.porelo.msgsluice.filter.decision.Redactor;
import org.fuin.porelo.msgsluice.filter.decision.Verdict;
import org.junit.jupiter.api.Test;

/**
 * The detectors' rates on source code, measured as {@link EncodedPayloadRatesTest} measures them on
 * prose: false positives against real code, and false negatives from 32, 48 and 64 characters of
 * payload per encoding, written into a line of Java.
 *
 * <p>The code is {@code code-corpus.txt}, frozen once from the Java sources committed in this
 * repository, main and test: each file split into blocks at its blank lines, each block cut into
 * snippets of 1 to 12 consecutive lines (the size of a suggested replacement in a review comment,
 * the length drawn at random with a seed taken from the file's path), each snippet dedented, those
 * with fewer than 20 visible characters dropped, and every second one kept. Left out are the files
 * whose lines are themselves test payloads - {@code Payloads.java}, {@code CredentialsTest.java},
 * {@code SshSignatureTest.java} and {@code PersonalDataTest.java}, which hold encoded blobs, keys,
 * card numbers and addresses on purpose - and this test itself, which did not exist yet.
 */
class CodeRatesTest {

    /** The detectors exactly as the default configuration builds them, for prose and for code. */
    static final DetectorRunner RUNNER = defaultRunner();

    private static DetectorRunner defaultRunner() {
        try {
            final Settings settings = Settings.load(null, Map.of("mail", "/m"));
            return new DetectorRunner(
                    settings.detectors(),
                    settings.codeDetectors(),
                    new Redactor(Redactor.Mode.PARTIAL));
        } catch (final ConfigException ex) {
            throw new IllegalStateException(ex);
        }
    }

    static final DecisionModel MODEL = new DecisionModel(DecisionConfig.defaults());

    static Verdict judge(final String code) {
        final List<Finding> findings =
                RUNNER.run(List.of(TextUnderCheck.code("parts[0].text", 0, code)));
        return MODEL.decide(findings, true);
    }

    /** The frozen corpus's size: a different count means the reference set itself changed. */
    static final int CORPUS_SNIPPETS = 1245;

    /** The corpus's snippets refused with the default thresholds, measured. */
    static final int REFUSED_MEASURED = 8;

    /** The code, frozen as a test resource, snippets separated by one blank line. */
    static List<String> codeCorpus() throws Exception {
        final String text;
        try (InputStream in = CodeRatesTest.class.getResourceAsStream("/code-corpus.txt")) {
            assertThat(in).as("code-corpus.txt on the test class path").isNotNull();
            text = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        final List<String> snippets =
                Arrays.stream(text.split("\n\n"))
                        .map(String::strip)
                        .filter(p -> !p.isEmpty())
                        .toList();
        assertThat(snippets).as("code corpus").hasSize(CORPUS_SNIPPETS);
        return snippets;
    }

    /**
     * The snippet with a line of Java holding the payload as a string literal, at a random line.
     */
    static String withPayload(final String snippet, final String payload, final Random random) {
        final List<String> lines = Arrays.asList(snippet.split("\n"));
        final int at = random.nextInt(lines.size() + 1);
        final String line = "final String data = \"" + payload + "\";";
        return String.join(
                "\n",
                java.util.stream.Stream.of(
                                lines.subList(0, at).stream(),
                                java.util.stream.Stream.of(line),
                                lines.subList(at, lines.size()).stream())
                        .flatMap(s -> s)
                        .toList());
    }

    @Test
    void codeIsAcceptedAndThirtyTwoCharactersOfEveryEncodingInItAreNot() throws Exception {
        final List<String> code = codeCorpus();
        final List<String> refused = code.stream().filter(t -> !judge(t).accepted()).toList();
        System.out.printf("code: %d snippets, %d refused%n", code.size(), refused.size());
        assertThat(refused.size())
                .as("false positives, %s", refused)
                .isLessThanOrEqualTo(REFUSED_MEASURED + 2);

        final Random random = new Random(42);
        for (final int length : new int[] {32, 48, 64}) {
            for (final Map.Entry<String, Function<Random, String>> g :
                    Payloads.generators(length).entrySet()) {
                int missed = 0;
                for (int i = 0; i < 500; i++) {
                    final String carrier = code.get(random.nextInt(code.size()));
                    if (judge(withPayload(carrier, g.getValue().apply(random), random))
                            .accepted()) {
                        missed++;
                    }
                }
                System.out.printf("%3d %-17s missed %3d / 500%n", length, g.getKey(), missed);
                assertThat(missed).as("%s at %d characters", g.getKey(), length).isLessThan(5);
            }
        }
    }
}
