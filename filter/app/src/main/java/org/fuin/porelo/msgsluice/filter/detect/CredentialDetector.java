package org.fuin.porelo.msgsluice.filter.detect;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.fuin.porelo.msgsluice.filter.decision.Severity;
import org.fuin.porelo.msgsluice.filter.decision.Stage;

/**
 * Credentials, found by what they are rather than by how they look: a credential can look like
 * ordinary text, so the encoded-data checks miss it. Each rule is a pattern, where one exists a
 * validation that the match really is one, and where the value alone says nothing a context word
 * near it. It reads the text as normalized, never the masked text of the encoded-data checks, so a
 * token inside a URL's query string is seen too.
 *
 * <p>One rule per category, {@code CREDENTIAL/<category>}: {@code decision.disabledRules} switches
 * a category off and {@code decision.severity.<category>} re-grades it. A value on the list of
 * known test values is reported as {@code INFO} only.
 */
public final class CredentialDetector implements ContentDetector {

    public static final String ID = "builtin/credentials@1";

    private static final String PREFIX = "CREDENTIAL/";

    /** How far around a value a context word counts, in characters. */
    private static final int CONTEXT = 40;

    private static final Pattern CONTEXT_WORD =
            Pattern.compile(
                    "(?i)\\b(?:api[ _-]?key|apikey|secret|token|access[ _-]?key|client[ _-]?secret"
                            + "|bearer|credential|auth)s?\\b");

    private record Rule(
            String category,
            Severity severity,
            Pattern pattern,
            int group,
            Predicate<Matcher> valid,
            boolean needsContext,
            String reason) {}

    private static final List<Rule> RULES =
            List.of(
                    new Rule(
                            "PRIVATE_KEY",
                            Severity.BLOCKING,
                            Pattern.compile(
                                    "-----BEGIN (?:[A-Z0-9]{1,16} ){0,4}PRIVATE KEY-----"
                                            + "(?:\\s|\\\\[nr])*+"
                                            + "(?:[A-Za-z0-9+/=]{40}|[A-Za-z0-9-]{1,40}: )"),
                            0,
                            m -> true,
                            false,
                            "A private key: its header and the start of its body. A private key"
                                    + " gives whoever holds it the identity it belongs to."),
                    new Rule(
                            "CLOUD_KEY",
                            Severity.BLOCKING,
                            Pattern.compile("\\b(?:AKIA|ASIA)[0-9A-Z]{16}\\b"),
                            0,
                            m -> true,
                            false,
                            "An AWS access key id."),
                    new Rule(
                            "CLOUD_KEY",
                            Severity.BLOCKING,
                            Pattern.compile(
                                    "(?i)(?:AccountKey|SharedAccessKey)=[A-Za-z0-9+/]{20,}={0,2}"),
                            0,
                            m -> true,
                            false,
                            "The key of an Azure connection string."),
                    new Rule(
                            "CLOUD_KEY",
                            Severity.BLOCKING,
                            Pattern.compile(
                                    "\"private_key\"\\s*:\\s*\"-----BEGIN (?:[A-Z0-9]{1,16} ){0,4}"
                                            + "PRIVATE KEY-----"),
                            0,
                            m -> true,
                            false,
                            "The private key of a key file, as a Google Cloud service account's."),
                    new Rule(
                            "VCS_TOKEN",
                            Severity.BLOCKING,
                            Pattern.compile(
                                    "\\b(?:gh[pousr]_[A-Za-z0-9]{36}|github_pat_[A-Za-z0-9_]{60,}"
                                            + "|glpat-[A-Za-z0-9_-]{20}|npm_[A-Za-z0-9]{36}"
                                            + "|pypi-[A-Za-z0-9_-]{50,})"),
                            0,
                            m -> true,
                            false,
                            "A token of a code host or a package registry."),
                    new Rule(
                            "JWT",
                            Severity.BLOCKING,
                            Pattern.compile(
                                    "(?<![A-Za-z0-9_-])eyJ[A-Za-z0-9_-]{8,}+\\.[A-Za-z0-9_-]{2,}+"
                                            + "\\.[A-Za-z0-9_-]{8,}+"),
                            0,
                            m -> jwtHeader(m.group()),
                            false,
                            "A signed JSON Web Token: whoever holds it acts as its subject until it"
                                    + " expires."),
                    new Rule(
                            "CONNECTION_STRING",
                            Severity.BLOCKING,
                            Pattern.compile(
                                    "(?i)(?<![a-z0-9+.-])[a-z][a-z0-9+.-]{0,30}+://"
                                            + "[^\\s/:@\"'`<>]{1,64}+:([^\\s/@\"'`<>]{1,128}+)@"
                                            + "[^\\s/\"'`<>]{1,253}+"),
                            1,
                            CredentialDetector::uriWithPassword,
                            false,
                            "An address that carries a user name and a password."),
                    new Rule(
                            "PASSWORD",
                            Severity.BLOCKING,
                            Pattern.compile(
                                    "(?i)(?:\\b|_)(?:password|passwd|passphrase)\\b[\"']?"
                                            + "(?:\\s+(?:is|was|will be)\\s*[:=]?|\\s*(?:=>|[:=]))"
                                            + "\\s*[\"'`]?([^\\s\"'`,;]{6,}+)"),
                            1,
                            m -> secretLike(m.group(1)) && !placeholder(m.group(1)),
                            false,
                            "A password, named as one beside its value."),
                    new Rule(
                            "SSH_AUTH",
                            Severity.BLOCKING,
                            Pattern.compile("\\bsshpass\\s+-p\\s*[\"']?([^\\s\"'`]{4,}+)"),
                            1,
                            m -> !placeholder(m.group(1)),
                            false,
                            "A password handed to ssh on the command line."),
                    new Rule(
                            "API_KEY_GENERIC",
                            Severity.BLOCKING,
                            Pattern.compile("(?<![A-Za-z0-9_+/-])[A-Za-z0-9_+/-]{20,}+={0,2}"),
                            0,
                            m -> mixed(m.group()),
                            true,
                            "A random-looking value of twenty characters or more next to a word"
                                    + " that names a key, a secret or a token."),
                    new Rule(
                            "CERTIFICATE",
                            Severity.SUSPICIOUS,
                            Pattern.compile("-----BEGIN CERTIFICATE-----"),
                            0,
                            m -> true,
                            false,
                            "The header of a certificate. It is public, but rarely belongs in a"
                                    + " message."),
                    new Rule(
                            "HASH",
                            Severity.SUSPICIOUS,
                            Pattern.compile(
                                    "\\$(?:2[aby]\\$\\d{2}\\$[./A-Za-z0-9]{53}"
                                            + "|argon2(?:id|i|d)\\$[^\\s]{20,}"
                                            + "|[56]\\$[^\\s$]{1,16}\\$[./A-Za-z0-9]{43,86})"),
                            0,
                            m -> true,
                            false,
                            "A password hash, which can be guessed against offline."));

    private final Set<String> dummyValues;

    /** {@code dummyValues} are known test values, compared exactly. */
    public CredentialDetector(final Set<String> dummyValues) {
        this.dummyValues = Set.copyOf(dummyValues);
    }

    @Override
    public String id() {
        return ID;
    }

    private static final Set<String> CATEGORIES =
            RULES.stream().map(Rule::category).collect(Collectors.toUnmodifiableSet());

    private static final Set<String> RULE_IDS =
            CATEGORIES.stream().map(c -> PREFIX + c).collect(Collectors.toUnmodifiableSet());

    /** Around a value, what is markup or punctuation and no part of it. */
    private static final Pattern TRIM = Pattern.compile("^[*_.:!?)\\]]+|[*_.:!?)\\]]+$");

    private static final Pattern SEPARATOR = Pattern.compile("[-_/+.=]");

    private static final Pattern NAMED_PLACEHOLDER =
            Pattern.compile("[A-Z_]+|password|secret|token|pass");

    @Override
    public Set<String> ruleIds() {
        return RULE_IDS;
    }

    @Override
    public Set<String> categories() {
        return CATEGORIES;
    }

    @Override
    public List<DetectorFinding> inspect(final TextUnderCheck under) {
        final String text = under.text();
        final List<DetectorFinding> findings = new ArrayList<>();
        for (final Rule rule : RULES) {
            final Matcher m = rule.pattern().matcher(text);
            while (m.find()) {
                if (!rule.valid().test(m) || (rule.needsContext() && !nearContext(text, m))) {
                    continue;
                }
                final int start = m.start(rule.group());
                final int end = m.end(rule.group());
                final boolean dummy = dummyValues.contains(m.group(rule.group()));
                findings.add(
                        new DetectorFinding(
                                PREFIX + rule.category(),
                                Stage.TEXT,
                                dummy ? Severity.INFO : rule.severity(),
                                rule.category(),
                                start,
                                end,
                                dummy ? "A known test value: " + rule.reason() : rule.reason()));
            }
        }
        return findings;
    }

    private static boolean nearContext(final String text, final Matcher m) {
        final String around =
                text.substring(
                        Math.max(0, m.start() - CONTEXT),
                        Math.min(text.length(), m.end() + CONTEXT));
        return CONTEXT_WORD.matcher(around).find();
    }

    /**
     * One stretch of at least sixteen characters between separators with letters of both cases and
     * a digit: what a generated value has, and a file name joined from words by dashes does not.
     */
    private static boolean mixed(final String value) {
        for (final String piece : SEPARATOR.split(value)) {
            if (piece.length() >= 16
                    && piece.chars().anyMatch(Character::isUpperCase)
                    && piece.chars().anyMatch(Character::isLowerCase)
                    && piece.chars().anyMatch(Character::isDigit)) {
                return true;
            }
        }
        return false;
    }

    /**
     * A letter and a digit or a symbol inside it: "hunter2" is a password, "stored" and "**typed**"
     * are words, and "join(...)" is code.
     */
    private static boolean secretLike(final String value) {
        final String inner = TRIM.matcher(value).replaceAll("");
        return inner.length() >= 6
                && inner.chars().anyMatch(Character::isLetter)
                && inner.chars().anyMatch(c -> !Character.isLetter(c))
                && inner.indexOf('(') < 0;
    }

    /**
     * {@code ${DB_PASSWORD}}, {@code <password>}, {@code ****}: where a value goes, not a value.
     */
    private static boolean placeholder(final String value) {
        return NAMED_PLACEHOLDER.matcher(value).matches()
                || value.startsWith("$")
                || value.startsWith("<")
                || value.startsWith("{")
                || value.startsWith("%")
                || value.contains("***");
    }

    private static boolean jwtHeader(final String token) {
        final String header = token.substring(0, token.indexOf('.'));
        try {
            final String json =
                    new String(Base64.getUrlDecoder().decode(header), StandardCharsets.UTF_8);
            return json.strip().startsWith("{") && json.contains("\"alg\"");
        } catch (final IllegalArgumentException ex) {
            return false;
        }
    }

    private static boolean uriWithPassword(final Matcher m) {
        try {
            final String userInfo = new URI(m.group()).getRawUserInfo();
            final int colon = userInfo == null ? -1 : userInfo.indexOf(':');
            return colon > 0
                    && colon < userInfo.length() - 1
                    && !placeholder(userInfo.substring(colon + 1));
        } catch (final URISyntaxException ex) {
            return false;
        }
    }
}
