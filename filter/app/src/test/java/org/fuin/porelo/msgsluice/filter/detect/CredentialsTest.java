package org.fuin.porelo.msgsluice.filter.detect;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.fuin.porelo.msgsluice.filter.ConfigException;
import org.fuin.porelo.msgsluice.filter.Settings;
import org.fuin.porelo.msgsluice.filter.decision.DecisionModel;
import org.fuin.porelo.msgsluice.filter.decision.Finding;
import org.fuin.porelo.msgsluice.filter.decision.Redactor;
import org.fuin.porelo.msgsluice.filter.decision.Verdict;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * A credential in a message is refused, judged by the default configuration as the filter runs it,
 * and its value never comes back in the answer. Each sample is a sentence an agent might write, and
 * each is built from pieces, so that no scanner reading this repository takes the source for a
 * leak.
 */
class CredentialsTest {

    /** A value of {@code n} characters that is no word, with letters, digits and both cases. */
    static String random(final int n) {
        final String alphabet = "Q7x2Lm9Kp4Rt8Vw3Zy6Bn5Hj1Gf0Dc";
        final StringBuilder b = new StringBuilder(n);
        for (int i = 0; i < n; i++) {
            b.append(alphabet.charAt((i * 7 + 3) % alphabet.length()));
        }
        return b.toString();
    }

    static final String ALG = "HS256"; // not-a-citation: the signing algorithm a JWT names

    static String jwt() {
        final java.util.Base64.Encoder url = java.util.Base64.getUrlEncoder().withoutPadding();
        return url.encodeToString(("{\"alg\":\"" + ALG + "\",\"typ\":\"JWT\"}").getBytes())
                + "."
                + url.encodeToString("{\"sub\":\"1\"}".getBytes())
                + "."
                + random(43);
    }

    /** The category, the value that must not come back, and the sentence carrying it. */
    static Stream<Arguments> credentials() {
        final String aws = "AK" + "IA" + "Q7X2LM9KP4RT8VW3";
        final String gh = "gh" + "p_" + random(36);
        final String gl = "gl" + "pat-" + random(20);
        final String jwt = jwt();
        final String pw = "hunter" + "2x";
        final String conn = "postgres://app:" + "S3cr" + "etPw9@db.internal:5432/orders";
        final String generic = random(32);
        final String bcrypt = "$2" + "b$12$" + random(53).replace('0', '.');
        return Stream.of(
                Arguments.of(
                        "PRIVATE_KEY",
                        random(64),
                        "Here it is:\n-----BEGIN RSA "
                                + "PRIVATE KEY-----\n"
                                + random(64)
                                + "\n-----END RSA PRIVATE KEY-----"),
                Arguments.of(
                        "PRIVATE_KEY",
                        random(64),
                        "The deploy key: -----BEGIN OPENSSH " + "PRIVATE KEY----- " + random(64)),
                Arguments.of("CLOUD_KEY", aws, "Sure, the key is " + aws + " for the bucket."),
                Arguments.of(
                        "CLOUD_KEY",
                        "AccountKey=" + random(44),
                        "Use DefaultEndpointsProtocol=https;AccountName=store;AccountKey="
                                + random(44)
                                + ";EndpointSuffix=core.windows.net to connect."),
                Arguments.of("VCS_TOKEN", gh, "My token for the repository is " + gh + "."),
                Arguments.of("VCS_TOKEN", gl, "The GitLab token " + gl + " still works."),
                Arguments.of("JWT", jwt, "Send it with the header Authorization: Bearer " + jwt),
                Arguments.of("PASSWORD", pw, "Log in as admin, my password is " + pw + " today."),
                Arguments.of("PASSWORD", pw, "Set DB_PASSWORD=" + pw + " in the environment."),
                Arguments.of("CONNECTION_STRING", "S3cr" + "etPw9", "Connect to " + conn + " now."),
                Arguments.of(
                        "API_KEY_GENERIC",
                        generic,
                        "The api key for the service is " + generic + ", keep it."),
                Arguments.of(
                        "SSH_AUTH", pw, "Run sshpass -p " + pw + " ssh deploy@host to get in."),
                Arguments.of("HASH", bcrypt, "The stored hash is " + bcrypt + " for that user."));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("credentials")
    void aCredentialInASentenceIsFoundAndNotRepeated(
            final String category, final String value, final String sentence) throws Exception {
        final Verdict verdict = judge(sentence, Map.of());

        assertThat(verdict.findings().stream().map(Finding::category))
                .as("categories found in: %s", sentence)
                .contains(category);
        assertThat(verdict.findings().toString())
                .as("the value itself is in no finding")
                .doesNotContain(value);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("credentials")
    void aCredentialIsGradedByItsCategoryAndGoneWithItsRuleSwitchedOff(
            final String category, final String value, final String sentence) throws Exception {
        final Verdict verdict = judge(sentence, Map.of());
        final String expected = category.equals("HASH") ? "SUSPICIOUS" : "BLOCKING";
        assertThat(
                        verdict.findings().stream()
                                .filter(f -> f.category().equals(category))
                                .map(f -> f.severity().name())
                                .distinct()
                                .toList())
                .as("severity of %s in: %s", category, sentence)
                .containsExactly(expected);
        if (expected.equals("BLOCKING")) {
            assertThat(verdict.accepted()).as("accepted: %s", sentence).isFalse();
        }

        final Verdict off =
                judge(sentence, Map.of("decision.disabledRules", "CREDENTIAL/" + category));
        assertThat(off.findings().stream().map(Finding::category))
                .as("with CREDENTIAL/%s switched off", category)
                .doesNotContain(category);
    }

    @Test
    void aCredentialInMetadataIsRefusedToo() throws Exception {
        final String aws = "AK" + "IA" + "Q7X2LM9KP4RT8VW3";
        final List<Finding> findings =
                runner(Map.of())
                        .run(List.of(new TextUnderCheck("metadata.note", -1, "key " + aws)));
        assertThat(findings.stream().map(Finding::category))
                .as("categories found in metadata")
                .contains("CLOUD_KEY");
    }

    @Test
    void whatOnlyLooksLikeACredentialIsNotOne() throws Exception {
        for (final String text :
                List.of(
                        "The password is stored in the vault and never logged.",
                        "Change the password, then sign in again.",
                        "The case is in detector-password-in-a-sentence.json, next to the others.",
                        "Where a host key or a passphrase is asked, the window shows it.",
                        "A token or a password is **typed** into a terminal on the machine.",
                        "Clone with https://user:TOKEN@host/repo.git and the token is written down.",
                        "passphrase = ''.join(secrets.choice(alphabet) for _ in range(4))",
                        "A file starting with -----BEGIN OPENSSH PRIVATE KEY----- is converted.",
                        "A password for `sshpass -p` and a key beside a word naming one are refused.",
                        "A token is a run of characters; the api key goes into the environment.",
                        "Connect to postgres://db.internal:5432/orders as the service user.",
                        "The private key stays on the machine; only the public key is shared.")) {
            assertThat(
                            judge(text, Map.of()).findings().stream()
                                    .map(Finding::ruleId)
                                    .filter(r -> r.startsWith("CREDENTIAL/"))
                                    .toList())
                    .as("credential findings in: %s", text)
                    .isEmpty();
        }
    }

    @Test
    void oneSegmentOfATokenIsNoSignedToken() throws Exception {
        final String header = jwt().substring(0, jwt().indexOf('.'));
        assertThat(
                        judge("The first segment " + header + " alone is no token.", Map.of())
                                .findings()
                                .stream()
                                .map(Finding::category)
                                .toList())
                .as("categories found for a lone header")
                .doesNotContain("JWT");
    }

    /** Ways a credential is written in code and configuration, which must not slip through. */
    static Stream<Arguments> writtenAsCode() {
        final String pw = "S3cr" + "etPw9x";
        final String body = random(64);
        return Stream.of(
                Arguments.of(
                        "CONNECTION_STRING", pw, "Use `postgres://app:" + pw + "@db:5432` here."),
                Arguments.of(
                        "CONNECTION_STRING", pw, "url = \"postgres://app:" + pw + "@db:5432\""),
                Arguments.of(
                        "CONNECTION_STRING", pw, "See <redis://u:" + pw + "@cache:6379> for it."),
                Arguments.of("CONNECTION_STRING", pw, "Postgres://app:" + pw + "@db:5432/orders"),
                Arguments.of("PASSWORD", pw, "{\"user\": \"app\", \"password\": \"" + pw + "\"}"),
                Arguments.of("PASSWORD", pw, "$config = ['password' => '" + pw + "'];"),
                Arguments.of(
                        "PRIVATE_KEY",
                        body,
                        "{\"key\": \"-----BEGIN RSA " + "PRIVATE KEY-----\\n" + body + "\\n\"}"),
                Arguments.of(
                        "CLOUD_KEY",
                        "-----BEGIN",
                        "{\"private_key\": \"-----BEGIN "
                                + "PRIVATE KEY-----\\nMII\", \"type\":"
                                + " \"service_account\"}"));
    }

    @ParameterizedTest(name = "{0}: {2}")
    @MethodSource("writtenAsCode")
    void aCredentialWrittenAsCodeIsFoundToo(
            final String category, final String value, final String text) throws Exception {
        final Verdict verdict = judge(text, Map.of());
        assertThat(verdict.findings().stream().map(Finding::category))
                .as("categories found in: %s", text)
                .contains(category);
        assertThat(verdict.findings().toString())
                .as("the value itself is in no finding")
                .doesNotContain(value.equals("-----BEGIN") ? "\u0000" : value);
    }

    /** Text an adversary shapes so that a rule takes seconds or overflows the stack. */
    static Stream<Arguments> shapedToBeSlow() {
        return Stream.of(
                Arguments.of("a run of 'a.'", "a.".repeat(32768)),
                Arguments.of("'eyJ-' repeated", "eyJ-".repeat(16384)),
                Arguments.of("a key header of many words", "-----BEGIN " + "A ".repeat(32765)),
                Arguments.of("schemes without an end", "ab+".repeat(21845) + "://"),
                Arguments.of("'password=' repeated", "password=".repeat(7281)));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("shapedToBeSlow")
    void textShapedToBeSlowIsReadInLinearTime(final String name, final String text) {
        final CredentialDetector detector = new CredentialDetector(Set.of());
        // A message whose check hangs holds up every message behind it in the pass.
        assertTimeoutPreemptively(
                Duration.ofSeconds(2),
                () -> detector.inspect(new TextUnderCheck("parts[0].text", 0, text)),
                "64K characters of " + name + " read within two seconds");
    }

    @Test
    void aKnownTestValueIsInformationOnly() throws Exception {
        final String example = "AK" + "IA" + "IOSFODNN7" + "EXAMPLE";
        final Verdict verdict = judge("The documented example " + example + " is fine.", Map.of());
        assertThat(
                        verdict.findings().stream()
                                .filter(f -> f.category().equals("CLOUD_KEY"))
                                .map(f -> f.severity().name())
                                .toList())
                .as("the documented example key")
                .containsExactly("INFO");
        assertThat(verdict.accepted()).as("a documented example is no leak").isTrue();
    }

    static Verdict judge(final String text, final Map<String, String> extra)
            throws ConfigException {
        final Settings settings = settings(extra);
        return new DecisionModel(settings.config().decision())
                .decide(
                        runner(extra).run(List.of(new TextUnderCheck("parts[0].text", 0, text))),
                        true);
    }

    static DetectorRunner runner(final Map<String, String> extra) throws ConfigException {
        final Settings settings = settings(extra);
        return new DetectorRunner(
                settings.detectors(), new Redactor(settings.config().redaction()));
    }

    static Settings settings(final Map<String, String> extra) throws ConfigException {
        final Map<String, String> cli = new HashMap<>(extra);
        cli.put("mail", "/m");
        return Settings.load(null, cli);
    }
}
