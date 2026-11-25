package org.fuin.porelo.msgsluice.filter.a2a;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.fuin.porelo.msgsluice.filter.ConfigException;
import org.fuin.porelo.msgsluice.filter.Settings;
import org.fuin.porelo.msgsluice.filter.decision.Finding;
import org.fuin.porelo.msgsluice.filter.decision.Severity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * A comment's parts and addressee, and the direction the host names: a comment is a reply in prose
 * or one review document, and what a task sends goes only to the review.
 */
class CommentEnvelopeTest {

    static final String PROSE = "The loop reads one element past the end of the list.";

    /** Porelo's own fixture of a submitted comment, as the review profile's tests jar holds it. */
    static final String SUBMISSION = read("review/submitted-comment.sarif.json");

    static final String TEXT = "A blank password reaches hash() here; refuse it before the lookup.";

    static String read(final String resource) {
        try (java.io.InputStream in =
                CommentEnvelopeTest.class.getClassLoader().getResourceAsStream(resource)) {
            if (in == null) {
                throw new IllegalStateException("No test resource " + resource);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (final java.io.IOException ex) {
            throw new java.io.UncheckedIOException(ex);
        }
    }

    static EnvelopeResult check(final String message, final boolean inbound) throws Exception {
        final Map<String, String> cli =
                inbound ? Map.of("mail", "/m", Settings.INBOUND, "true") : Map.of("mail", "/m");
        final CheckConfig config = Settings.load(null, cli).config().check();
        return new EnvelopeCheck(config)
                .check(new A2aReader().parse(message.getBytes(StandardCharsets.UTF_8)));
    }

    static String comment(final String to, final String parts) {
        return """
               {"messageId":"m-1","role":"ROLE_AGENT","parts":[%s],
                "metadata":{"to":"%s","kind":"comment"}}"""
                .formatted(parts, to);
    }

    @Test
    void aSubmissionToTheReviewPasses() throws Exception {
        final EnvelopeResult result =
                check(comment("review", "{\"data\":" + SUBMISSION + "}"), false);

        assertThat(result.findings())
                .as("a submitted comment in its one data part, addressed to the review")
                .isEmpty();
        assertThat(result.texts())
                .as("the comment's text is judged by the detectors")
                .anySatisfy(t -> assertThat(t.text()).isEqualTo(TEXT));
    }

    @Test
    void aReplyInProseStillPasses() throws Exception {
        final EnvelopeResult result =
                check(comment("review", "{\"text\":\"" + PROSE + "\"}"), false);

        assertThat(result.findings()).as("a reply of one text part").isEmpty();
    }

    @Test
    void aDataPartBesideATextPartIsRefusedByItsOwnRule() throws Exception {
        final EnvelopeResult result =
                check(
                        comment(
                                "review",
                                "{\"text\":\"" + PROSE + "\"},{\"data\":" + SUBMISSION + "}"),
                        false);

        assertThat(result.findings())
                .as("a comment mixing parts")
                .extracting(Finding::ruleId)
                .containsExactly(EnvelopeCheck.COMMENT_SHAPE);
    }

    @Test
    void twoDataPartsAreRefusedByTheSameRule() throws Exception {
        final String data = "{\"data\":" + SUBMISSION + "}";
        final EnvelopeResult result = check(comment("review", data + "," + data), false);

        assertThat(result.findings())
                .as("a comment of two documents")
                .extracting(Finding::ruleId)
                .containsExactly(EnvelopeCheck.COMMENT_SHAPE);
    }

    @Test
    void aSubmissionToAnyoneButTheReviewIsRefused() throws Exception {
        final EnvelopeResult result =
                check(comment("alice", "{\"data\":" + SUBMISSION + "}"), false);

        assertThat(result.findings())
                .as("a comment a task addresses to a person")
                .extracting(Finding::ruleId)
                .containsExactly(EnvelopeCheck.COMMENT_ADDRESSEE);
        assertThat(result.findings())
                .as("refused whether or not the filter blocks")
                .allMatch(f -> f.severity() == Severity.BLOCKING);
    }

    @Test
    void aDataPartOfAnyOtherKindIsStillRefused() throws Exception {
        final String message =
                """
                {"messageId":"m-1","role":"ROLE_AGENT","parts":[{"data":%s}],
                 "metadata":{"to":"review","kind":"note"}}"""
                        .formatted(SUBMISSION);

        assertThat(check(message, false).findings())
                .as("only a comment carries a data part")
                .extracting(Finding::ruleId)
                .contains("ENVELOPE/DATA_PART_NOT_ALLOWED");
    }

    @Test
    void aSubmissionNamedInboundIsRefused() throws Exception {
        final EnvelopeResult result =
                check(comment("review", "{\"data\":" + SUBMISSION + "}"), true);

        assertThat(result.findings())
                .as("a message from a forge carries a page, never a submission")
                .extracting(Finding::ruleId)
                .contains(ReviewProfileCheck.DIRECTION);
    }

    @Test
    void theDirectionIsNamedOnTheCommandLineOnly(@TempDir final Path tmp) throws Exception {
        final Path file = tmp.resolve("filter.properties");
        Files.writeString(file, "inbound=true\n");

        assertThatThrownBy(() -> Settings.load(file, Map.of("mail", "/m")))
                .as("a configuration file serves every direction")
                .isInstanceOf(ConfigException.class)
                .hasMessageContaining("--inbound");
        assertThat(Settings.load(null, Map.of("mail", "/m")).config().check().inbound())
                .as("the default is what a task sends, the stricter direction")
                .isFalse();
    }
}
