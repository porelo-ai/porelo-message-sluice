package org.fuin.porelo.msgsluice.filter;

import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.fuin.porelo.msgsluice.filter.MessageSluice.FileResult;
import org.fuin.porelo.msgsluice.filter.MessageSluice.Outcome;
import org.fuin.porelo.msgsluice.filter.decision.Finding;
import org.fuin.porelo.msgsluice.filter.decision.Stage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Review comments carrying a profile 2 document, end to end through a mailbox. The three passing
 * shapes in {@code review/} are Porelo's own fixtures, copied unchanged; each one in {@code
 * review/refused/} is this repository's, made from them with one fault, and is refused by exactly
 * one envelope rule, the one its name gives; its source passes, so that one fault is what refuses
 * it.
 */
class ReviewFixturesTest {

    static final Path REVIEW = Path.of("src/test/resources/review");

    /** 48 characters of base64, short enough to escape the detectors' URL masking. */
    static final String BASE64_48 = "UEsDBBQAAAAIAGx0Mlt3b3JkL2RvY3VtZW50LnhtbKxVS27b";

    /** A refused fixture's name, after its direction, starts with what its rule is for. */
    static final Map<String, String> RULES =
            Map.of(
                    "budget", "PROFILE/BUDGET_EXCEEDED",
                    "unknown-field", "PROFILE/UNKNOWN_FIELD",
                    "missing", "PROFILE/MISSING_FIELD",
                    "malformed", "PROFILE/VALUE_MALFORMED",
                    "character", "PROFILE/FORBIDDEN_CHARACTER",
                    "direction", "PROFILE/WRONG_DIRECTION",
                    "producer", "PROFILE/PRODUCER_NOT_ALLOWED",
                    "region", "PROFILE/REGION_TOO_LARGE",
                    "replacement", "PROFILE/REPLACEMENT_OUTSIDE_ANCHOR",
                    "text", "PROFILE/TEXT_TOO_LONG");

    @TempDir Path tmp;

    /** A task's comment to the review, its one data part the document as it stands in the file. */
    static String comment(final String id, final String document) {
        return """
               {"messageId":"%s","role":"ROLE_AGENT","parts":[{"data":%s}],
                "metadata":{"to":"review","kind":"comment"}}"""
                .formatted(id, document);
    }

    FileResult run(
            final String name, final String document, final boolean inbound, final boolean blocking)
            throws Exception {
        final Mailbox mail = new Mailbox(tmp.resolve(name + inbound + blocking));
        mail.deliver(name + ".json", comment(name, document));
        final MessageSluice.RunSummary summary =
                mail.sluice(
                                "blocking",
                                String.valueOf(blocking),
                                Settings.INBOUND,
                                String.valueOf(inbound))
                        .runOnce();
        assertThat(summary.results()).as(name).hasSize(1);
        return summary.results().get(0);
    }

    static Stream<String> passing() {
        return Stream.of("forge-page", "submitted-comment", "submitted-reply");
    }

    @ParameterizedTest
    @MethodSource("passing")
    void eachPassingShapePassesInItsDirectionWithItsBytesKept(final String name) throws Exception {
        final String document = read(REVIEW.resolve(name + ".sarif.json"));
        final boolean inbound = name.startsWith("forge-");
        final Mailbox mail = new Mailbox(tmp);
        final String message = comment(name, document);
        mail.deliver(name + ".json", message);

        final FileResult result =
                mail.sluice("blocking", "true", Settings.INBOUND, String.valueOf(inbound))
                        .runOnce()
                        .results()
                        .get(0);

        assertThat(result.findings()).as("%s in its direction", name).isEmpty();
        assertThat(result.outcome()).as(name).isEqualTo(Outcome.ACCEPTED);
        assertThat(Files.readString(mail.accepted().resolve(name + ".json")))
                .as("the accepted file holds exactly the bytes that came in")
                .isEqualTo(message);
    }

    @ParameterizedTest
    @MethodSource("passing")
    void eachPassingShapeIsRefusedInTheOtherDirection(final String name) throws Exception {
        final boolean inbound = !name.startsWith("forge-");

        final FileResult result =
                run(name, read(REVIEW.resolve(name + ".sarif.json")), inbound, false);

        assertThat(result.outcome())
                .as("%s, refused even while the filter only reports", name)
                .isEqualTo(Outcome.REJECTED);
        assertThat(envelopeRules(result)).as(name).containsExactly("PROFILE/WRONG_DIRECTION");
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void eachRefusedFixtureIsRefusedByExactlyItsOwnRule(final boolean blocking) throws Exception {
        final List<Path> fixtures;
        try (Stream<Path> files = Files.list(REVIEW.resolve("refused"))) {
            fixtures = files.sorted().toList();
        }
        assertThat(fixtures).as("one fixture per refused condition").hasSizeGreaterThan(25);
        assertThat(fixtures)
                .as("every rule has a fixture")
                .extracting(ReviewFixturesTest::rule)
                .containsAll(RULES.values());
        for (final Path fixture : fixtures) {
            final String name = fixture.getFileName().toString().replace(".sarif.json", "");

            final FileResult result = run(name, read(fixture), name.startsWith("in-"), blocking);

            assertThat(result.outcome()).as(name).isEqualTo(Outcome.REJECTED);
            assertThat(envelopeRules(result)).as(name).containsExactly(rule(fixture));
        }
    }

    @Test
    void aSubmittedSuggestionCarryingBase64IsHeldAsADetectorFinding() throws Exception {
        final String document =
                read(REVIEW.resolve("submitted-comment.sarif.json"))
                        .replace(
                                "final Account account = accounts.find(name);",
                                "final String k = \\\"UEsDBBQAAAAIAGx0Mlt3b3JkL2RvY3Vt\\\";");
        assertThat(document).as("the payload is in the replacement").contains("UEsDBBQA");

        final FileResult reporting = run("blob", document, false, false);
        final FileResult blocking = run("blob", document, false, true);

        assertThat(envelopeRules(blocking)).as("the document itself is well formed").isEmpty();
        assertThat(blocking.findings())
                .as("32 characters of base64 in the suggested code")
                .anyMatch(f -> f.ruleId().startsWith("ENCODING/"));
        assertThat(blocking.outcome()).as("blocking").isEqualTo(Outcome.REJECTED);
        assertThat(reporting.outcome())
                .as("reporting only: the host holds it for a person")
                .isEqualTo(Outcome.ACCEPTED);
        assertThat(String.join("\n", blocking.findings().stream().map(Finding::reason).toList()))
                .as("no reason repeats the payload")
                .doesNotContain("UEsDBBQA");
    }

    @Test
    void aFieldNameAnAgentChoseNeverLeavesInAFinding() throws Exception {
        final String key = "ghp_abcdefghijklmnopqrstuvwxyz0123";
        final String document =
                read(REVIEW.resolve("submitted-comment.sarif.json"))
                        .replaceFirst("\\{", "{\"" + key + "\": 1, ");
        final Mailbox mail = new Mailbox(tmp);
        mail.deliver("key.json", comment("key", document));

        final FileResult result = mail.sluice("blocking", "true").runOnce().results().get(0);

        assertThat(envelopeRules(result))
                .as("an unknown field")
                .containsExactly("PROFILE/UNKNOWN_FIELD");
        assertThat(result.findings())
                .as("named by its position, never by the name its writer chose")
                .allSatisfy(f -> assertThat(String.valueOf(f.location())).doesNotContain("ghp_"));
        assertThat(mail.everythingThatLeaves())
                .as("nothing that leaves the filter holds the name")
                .doesNotContain(key);
    }

    @Test
    void aCredentialInAThreadIdIsFoundByTheDetectors() throws Exception {
        final String document =
                read(REVIEW.resolve("submitted-reply.sarif.json"))
                        .replace(
                                "\"1834527690\"",
                                "\"" + "ghp_" + "0123456789" + "abcdefghijklmnopqrstuvwxyz\"");

        final FileResult result = run("reply", document, false, true);

        assertThat(envelopeRules(result)).as("the id has an id's shape").isEmpty();
        assertThat(result.findings())
                .as("a token in porelo/replyTo")
                .anyMatch(f -> f.ruleId().startsWith("CREDENTIAL/"));
        assertThat(result.outcome()).isEqualTo(Outcome.REJECTED);
    }

    @Test
    void aPayloadInARepositorysPathSegmentIsFoundByTheDetectors() throws Exception {
        final String segment = BASE64_48;
        final String document =
                read(REVIEW.resolve("submitted-comment.sarif.json"))
                        .replace(
                                "https://github.com/shop/app",
                                "https://github.com/" + segment + "/app");
        assertThat(document).as("in both URLs").contains(segment + "/app/pull/42");

        final FileResult result = run("segment", document, false, true);

        assertThat(envelopeRules(result)).as("the URLs have the profile's shape").isEmpty();
        assertThat(result.findings())
                .as("48 characters of base64 as a path segment, which URL masking would hide")
                .anyMatch(f -> f.ruleId().startsWith("ENCODING/"));
        assertThat(result.outcome()).isEqualTo(Outcome.REJECTED);
    }

    /**
     * Shapes a forge may give its repository and pull request, each with the profile's bounds at
     * their edge.
     */
    @ParameterizedTest
    @CsvSource({
        "https://forge.example:8443/shop/app, /pull/42",
        "https://forge.example:65535/shop/app, /pull/42",
        "https://forge.example/a/b/c/d/e/f/g/h, /pull/42",
        "https://forge.example/group/sub/app, /-/merge_requests/42",
        "https://forge.example/shop/app, /pulls/42",
        "https://forge.example/shop/app, /42",
        "https://forge.example/shop/app, /a/b/42",
        "https://forge.example/shop/the-app_v2.x~1, /pull/42",
    })
    void eachBoundedForgeShapeIsAdmitted(final String repository, final String pullRequest)
            throws Exception {
        final FileResult result = run("shape", withUrls(repository, repository + pullRequest));

        assertThat(envelopeRules(result))
                .as(repository + pullRequest + " has the profile's shape")
                .isEmpty();
        assertThat(result.outcome())
                .as("nothing else in it is refused")
                .isEqualTo(Outcome.ACCEPTED);
    }

    /** A path segment of the profile's longest length is admitted. */
    @Test
    void aSegmentOfOneHundredCharactersIsAdmitted() throws Exception {
        final String repository = "https://forge.example/shop/" + "app-".repeat(25);

        final FileResult result = run("long", withUrls(repository, repository + "/pull/42"));

        assertThat(envelopeRules(result)).as("100 characters bound a segment").isEmpty();
    }

    /** A port beyond 65535 or written with a leading zero is no port. */
    @ParameterizedTest
    @CsvSource({"65536", "08443", "0"})
    void aPortOutsideItsRangeIsRefused(final String port) throws Exception {
        final String repository = "https://forge.example:" + port + "/shop/app";

        final FileResult result = run("port", withUrls(repository, repository + "/pull/42"));

        assertThat(envelopeRules(result))
                .as("port " + port + " refuses only the repository, not its pull request again")
                .containsExactly("PROFILE/VALUE_MALFORMED");
        assertThat(result.findings())
                .as("one refusal, at the repository")
                .filteredOn(f -> f.stage() == Stage.ENVELOPE)
                .singleElement()
                .satisfies(f -> assertThat(f.location()).contains("repositoryUri"));
    }

    /** A host is DNS labels: none starts or ends with a hyphen, and none is empty. */
    @ParameterizedTest
    @CsvSource({"-forge.example", "forge-.example", "forge..example", "forge.example."})
    void aHostThatIsNoDnsNameIsRefused(final String host) throws Exception {
        final String repository = "https://" + host + "/shop/app";

        final FileResult result = run("host", withUrls(repository, repository + "/pull/42"));

        assertThat(envelopeRules(result))
                .as(host + " is no DNS name")
                .containsExactly("PROFILE/VALUE_MALFORMED");
    }

    /** A pull request under a repository whose URL only begins like its own is refused. */
    @Test
    void aPullRequestUnderALongerRepositoryNameIsRefused() throws Exception {
        final FileResult result =
                run(
                        "prefix",
                        withUrls(
                                "https://forge.example/shop/app",
                                "https://forge.example/shop/application/pull/42"));

        assertThat(result.findings())
                .as("the repository is shop/app, the pull request's shop/application")
                .filteredOn(f -> f.stage() == Stage.ENVELOPE)
                .singleElement()
                .satisfies(
                        f -> {
                            assertThat(f.ruleId()).isEqualTo("PROFILE/VALUE_MALFORMED");
                            assertThat(f.location()).contains("porelo/pullRequest");
                        });
    }

    /** A payload in the host alone, which no path segment carries, reaches the detectors. */
    @Test
    void aPayloadInTheHostIsFoundByTheDetectors() throws Exception {
        final String repository = "https://" + BASE64_48 + ".example/shop/app";

        final FileResult result = run("host", withUrls(repository, repository + "/pull/42"));

        assertThat(envelopeRules(result)).as("a DNS label of 48 characters is a host").isEmpty();
        assertThat(result.findings())
                .as("48 characters of base64 as the host")
                .anyMatch(
                        f ->
                                f.ruleId().startsWith("ENCODING/")
                                        && requireNonNull(f.location()).contains("repositoryUri"));
    }

    /** A payload in a segment only the pull request carries reaches the detectors. */
    @Test
    void aPayloadInAPullRequestsOwnSegmentIsFoundByTheDetectors() throws Exception {
        final String repository = "https://forge.example/shop/app";

        final FileResult result =
                run("tail", withUrls(repository, repository + "/" + BASE64_48 + "/42"));

        assertThat(envelopeRules(result)).as("the pull request has the profile's shape").isEmpty();
        assertThat(result.findings())
                .as("48 characters of base64 after the repository")
                .anyMatch(
                        f ->
                                f.ruleId().startsWith("ENCODING/")
                                        && requireNonNull(f.location())
                                                .contains("porelo/pullRequest"));
    }

    /**
     * A payload cut into segments each too short for a detector is found in the repository's whole
     * path, judged as one text.
     */
    @Test
    void aPayloadSpreadOverShortSegmentsIsFoundInTheWholePath() throws Exception {
        final String payload = BASE64_48.repeat(6).substring(0, 8 * 31);
        final StringBuilder path = new StringBuilder();
        for (int i = 0; i < 8; i++) {
            path.append('/').append(payload, i * 31, i * 31 + 31);
        }
        final String repository = "https://forge.example" + path;

        final FileResult result = run("spread", withUrls(repository, repository + "/pull/42"));

        assertThat(envelopeRules(result)).as("8 segments of 31 characters").isEmpty();
        assertThat(result.findings())
                .as("the whole path, held as one text, carries the payload")
                .anyMatch(
                        f ->
                                requireNonNull(f.location()).contains("(host and path)")
                                        && f.stage() != Stage.ENVELOPE);
        assertThat(result.outcome()).isEqualTo(Outcome.REJECTED);
    }

    /**
     * A host and path of fewer than 64 characters, whose base64 runs are each shorter than 32,
     * passes: the limit the detectors' document states, held so it is measured. With the next 64
     * characters the entropy window holds the whole path.
     */
    @Test
    void aPayloadCutByDotsIntoShortPiecesPassesAsTheDetectorsDocumentStates() throws Exception {
        final String repository =
                "https://forge.example/shop/"
                        + BASE64_48.substring(0, 15)
                        + "."
                        + BASE64_48.substring(15, 30);
        assertThat(repository.length() - "https://".length())
                .as("host and path stay below the entropy window")
                .isLessThan(64);

        final FileResult result = run("dots", withUrls(repository, repository + "/pull/42"));

        assertThat(result.findings())
                .as(
                        "30 characters of base64 in pieces of 15, in 50 of host and path, reach no threshold")
                .isEmpty();
        assertThat(result.outcome()).isEqualTo(Outcome.ACCEPTED);
    }

    static String withUrls(final String repository, final String pullRequest) throws Exception {
        final String document = read(REVIEW.resolve("submitted-comment.sarif.json"));
        assertThat(document)
                .as("the fixture names its repository and pull request once each")
                .contains("\"https://github.com/shop/app\"")
                .contains("\"https://github.com/shop/app/pull/42\"");
        return document.replace(
                        "\"https://github.com/shop/app/pull/42\"", "\"" + pullRequest + "\"")
                .replace("\"https://github.com/shop/app\"", "\"" + repository + "\"");
    }

    FileResult run(final String name, final String document) throws Exception {
        return run(name, document, false, true);
    }

    static String rule(final Path fixture) {
        final String name = fixture.getFileName().toString();
        final String what = name.substring(name.indexOf('-') + 1);
        return RULES.entrySet().stream()
                .filter(e -> what.startsWith(e.getKey() + "-"))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No rule in the name " + name));
    }

    static List<String> envelopeRules(final FileResult result) {
        return result.findings().stream()
                .filter(f -> f.stage() == Stage.ENVELOPE)
                .map(Finding::ruleId)
                .distinct()
                .toList();
    }

    static String read(final Path file) throws Exception {
        return Files.readString(file, StandardCharsets.UTF_8);
    }
}
