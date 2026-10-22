package org.fuin.porelo.msgsluice.filter.detect;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.fuin.porelo.msgsluice.filter.decision.Severity;
import org.fuin.porelo.msgsluice.filter.decision.Stage;
import org.jspecify.annotations.Nullable;

/**
 * Personal and financial data, found by pattern and, where one exists, by its checksum: the
 * checksum is what tells a card number or an IBAN from any other run of digits. What may be a
 * legitimate part of a business process - an e-mail address, a phone number - is suspicious, and a
 * project with a strict data-protection reading grades it blocking.
 *
 * <p>One rule per category, {@code PERSONAL/<category>}, switched off and re-graded as every rule
 * is. Every pattern is bounded, and those that start inside a run are anchored by a look-behind, so
 * text shaped to make one slow is read in linear time.
 */
public final class PersonalDataDetector implements ContentDetector {

    /** Names this detector and its version in findings and receipts. */
    public static final String ID = "builtin/personal-data@1";

    private static final String PREFIX = "PERSONAL/";

    private static final Pattern SEPARATORS = Pattern.compile("[ .-]");

    /** How far before a value a context word counts, in characters. */
    private static final int CONTEXT = 40;

    private record Rule(
            String category,
            Severity severity,
            Pattern pattern,
            int group,
            Predicate<Matcher> valid,
            @Nullable Pattern contextBefore,
            @Nullable Pattern notBefore,
            String reason) {}

    private static final Pattern NATIONAL_ID_CONTEXT =
            Pattern.compile("(?i)\\b(?:ssn|social security)");

    /** "Temurin 21.0.8.1", "snapshot 181.2.0.3": a version, not an address. */
    private static final Pattern VERSION_WORD =
            Pattern.compile(
                    "(?i)\\b(?:temurin|graalvm|ce|jdk|java|version|release|snapshot|build)\\s*$");

    private static final List<Rule> RULES =
            List.of(
                    new Rule(
                            "CREDIT_CARD",
                            Severity.BLOCKING,
                            Pattern.compile(
                                    "(?<![\\d.,-])\\d(?:[ .-]?\\d){12,18}(?![\\d.,/-]?\\d)"),
                            0,
                            m -> card(digits(m.group())),
                            null,
                            null,
                            "A payment card number: its digits pass the card checksum."),
                    new Rule(
                            "IBAN",
                            Severity.BLOCKING,
                            Pattern.compile(
                                    "(?<![A-Za-z0-9])[A-Za-z]{2}\\d{2}(?: ?[A-Za-z0-9]{4}){2,7}+"
                                            + "(?: ?[A-Za-z0-9]{1,3})?(?![A-Za-z0-9])"),
                            0,
                            m ->
                                    ibanShaped(m.group())
                                            && iban(
                                                    m.group()
                                                            .replace(" ", "")
                                                            .toUpperCase(Locale.ROOT)),
                            null,
                            null,
                            "An IBAN: it passes the bank account checksum."),
                    new Rule(
                            "NATIONAL_ID",
                            Severity.BLOCKING,
                            Pattern.compile(
                                    "(?<![\\d.])756(?:\\.\\d{4}\\.\\d{4}\\.\\d{2}|\\d{10})"
                                            + "(?![\\d.]?\\d)"),
                            0,
                            m -> ean13(digits(m.group())),
                            null,
                            null,
                            "A Swiss social insurance number: it passes its checksum."),
                    new Rule(
                            "NATIONAL_ID",
                            Severity.BLOCKING,
                            Pattern.compile("(?<![\\d-])(\\d{3})-(\\d{2})-(\\d{4})(?![\\d-])"),
                            0,
                            PersonalDataDetector::ssn,
                            NATIONAL_ID_CONTEXT,
                            null,
                            "A US social security number, named as one."),
                    new Rule(
                            "EMAIL",
                            Severity.SUSPICIOUS,
                            Pattern.compile(
                                    "(?<![\\p{L}0-9._%+-])[\\p{L}0-9._%+-]{1,64}+"
                                            + "@([A-Za-z0-9.-]{1,253}+)(?!:\\S)"),
                            0,
                            m -> mailDomain(m.group(1)),
                            null,
                            null,
                            "An e-mail address."),
                    new Rule(
                            "PHONE",
                            Severity.SUSPICIOUS,
                            Pattern.compile(
                                    "(?<![\\w+])\\+\\d{1,3}(?:[ .-]?\\(?\\d{1,4}\\)?){2,6}+(?![\\d])"),
                            0,
                            m -> digitCount(m.group(), 8, 15),
                            null,
                            null,
                            "An international phone number."),
                    new Rule(
                            "PHONE",
                            Severity.SUSPICIOUS,
                            Pattern.compile(
                                    "(?i)\\b(?:phone|tel|mobile|cell|fax|call (?:me|him|her|us) (?:at|on))\\b"
                                            + "[^\\d\\n(]{0,15}+(\\(?\\d[\\d ().-]{5,18}\\d)"),
                            1,
                            m -> digitCount(m.group(1), 7, 15),
                            null,
                            null,
                            "A phone number, named as one."),
                    new Rule(
                            "IP_ADDRESS",
                            Severity.SUSPICIOUS,
                            Pattern.compile(
                                    "(?<![A-Za-z\\d.])(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})"
                                            + "(?![\\d]|\\.\\d|[+-]\\d)"),
                            0,
                            PersonalDataDetector::publicAddress,
                            null,
                            VERSION_WORD,
                            "A public IP address, which can point at a person's connection."),
                    new Rule(
                            "DATE_OF_BIRTH",
                            Severity.SUSPICIOUS,
                            Pattern.compile(
                                    "(?i)\\b(?:born(?: on)?|date of birth|d\\.o\\.b\\.?|dob|birthday)\\b"
                                            + "[^\\d\\n]{0,15}+((?:\\d{1,2}[./-]\\d{1,2}[./-](?:19|20)\\d{2})"
                                            + "|(?:(?:19|20)\\d{2}-\\d{2}-\\d{2})"
                                            + "|(?:\\d{1,2}\\.? [A-Z][a-z]{2,8} (?:19|20)\\d{2}))"),
                            1,
                            m -> true,
                            null,
                            null,
                            "A date of birth, named as one."),
                    new Rule(
                            "POSTAL_ADDRESS",
                            Severity.INFO,
                            Pattern.compile(
                                    "(?<!\\d)\\d{1,5}[a-z]? ([A-Z][a-z]{2,20}(?: [A-Z][a-z]{2,20})?"
                                            + " (?:Street|Avenue|Road|Lane|Boulevard|Drive|Way))\\b"
                                            + "|\\b([A-Z][a-zäöü]{2,20}(?:strasse|straße|gasse|weg|platz))"
                                            + " \\d{1,4}[a-z]?\\b"),
                            0,
                            m -> true,
                            null,
                            null,
                            "A postal address."));

    private static final Set<String> CATEGORIES =
            RULES.stream().map(Rule::category).collect(Collectors.toUnmodifiableSet());

    private static final Set<String> RULE_IDS =
            CATEGORIES.stream().map(c -> PREFIX + c).collect(Collectors.toUnmodifiableSet());

    /** Domains reserved for documentation (RFC 2606), which hold nobody's address. */
    private static final Pattern RESERVED_DOMAIN =
            Pattern.compile(
                    "(?i)(?:^|\\.)(?:example\\.(?:com|org|net)|example|test|invalid|localhost)$");

    private final Set<String> dummyValues;

    /** {@code dummyValues} are known test values, compared without spaces, dots and dashes. */
    public PersonalDataDetector(final Set<String> dummyValues) {
        this.dummyValues =
                dummyValues.stream()
                        .map(PersonalDataDetector::compact)
                        .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public String id() {
        return ID;
    }

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
                if (!rule.valid().test(m)
                        || !contextBefore(rule, text, m.start())
                        || notBefore(rule, text, m.start())) {
                    continue;
                }
                final int group = m.group(rule.group()) == null ? 0 : rule.group();
                final boolean dummy = dummyValues.contains(compact(m.group(group)));
                findings.add(
                        new DetectorFinding(
                                PREFIX + rule.category(),
                                Stage.TEXT,
                                dummy ? Severity.INFO : rule.severity(),
                                rule.category(),
                                m.start(group),
                                m.end(group),
                                dummy ? "A known test value: " + rule.reason() : rule.reason()));
            }
        }
        return findings;
    }

    private static boolean notBefore(final Rule rule, final String text, final int start) {
        return rule.notBefore() != null
                && rule.notBefore()
                        .matcher(text.substring(Math.max(0, start - CONTEXT), start))
                        .find();
    }

    private static boolean contextBefore(final Rule rule, final String text, final int start) {
        return rule.contextBefore() == null
                || rule.contextBefore()
                        .matcher(text.substring(Math.max(0, start - CONTEXT), start))
                        .find();
    }

    private static String compact(final String value) {
        return SEPARATORS.matcher(value).replaceAll("");
    }

    private static String digits(final String value) {
        final StringBuilder b = new StringBuilder(value.length());
        value.chars().filter(Character::isDigit).forEach(c -> b.append((char) c));
        return b.toString();
    }

    private static boolean digitCount(final String value, final int min, final int max) {
        final long n = value.chars().filter(Character::isDigit).count();
        return n >= min && n <= max;
    }

    private static boolean luhn(final String digits) {
        int sum = 0;
        for (int i = 0; i < digits.length(); i++) {
            int d = digits.charAt(digits.length() - 1 - i) - '0';
            if (i % 2 == 1) {
                d *= 2;
                if (d > 9) {
                    d -= 9;
                }
            }
            sum += d;
        }
        return sum % 10 == 0;
    }

    /** The Luhn checksum and the prefix and length of a card network that issues such numbers. */
    private static boolean card(final String d) {
        if (!luhn(d) || d.chars().distinct().count() < 3) {
            return false;
        }
        final int two = Integer.parseInt(d.substring(0, 2));
        final int four = Integer.parseInt(d.substring(0, 4));
        final int len = d.length();
        return (d.startsWith("4") && (len == 13 || len == 16 || len == 19))
                || (((two >= 51 && two <= 55) || (four >= 2221 && four <= 2720)) && len == 16)
                || ((two == 34 || two == 37) && len == 15)
                || ((d.startsWith("6011") || d.startsWith("65")) && len >= 16)
                || (two == 35 && len >= 16);
    }

    /**
     * One case throughout and an account of mostly digits: two capitals and two digits before words
     * pass MOD-97 once in 97 tries, and real account numbers are not words.
     */
    private static boolean ibanShaped(final String value) {
        final String compact = value.replace(" ", "");
        final boolean upper =
                compact.chars().filter(Character::isLetter).allMatch(Character::isUpperCase);
        final boolean lower =
                compact.chars().filter(Character::isLetter).allMatch(Character::isLowerCase);
        final String account = compact.substring(4);
        return (upper || lower)
                && account.chars().filter(Character::isDigit).count() * 2 >= account.length();
    }

    private static boolean iban(final String iban) {
        if (iban.length() >= 15 && iban.length() <= 34 && mod97(iban)) {
            return true;
        }
        // A trailing four-character group after a space is swallowed by the pattern.
        for (int len = Math.min(32, (iban.length() - 1) & ~3); len >= 16; len -= 4) {
            if (mod97(iban.substring(0, len))) {
                return true;
            }
        }
        return false;
    }

    private static boolean mod97(final String iban) {
        final String moved = iban.substring(4) + iban.substring(0, 4);
        final StringBuilder digits = new StringBuilder();
        for (final char c : moved.toCharArray()) {
            digits.append(Character.isLetter(c) ? String.valueOf(c - 'A' + 10) : String.valueOf(c));
        }
        return new BigInteger(digits.toString()).mod(BigInteger.valueOf(97)).intValue() == 1;
    }

    private static boolean ean13(final String d) {
        int sum = 0;
        for (int i = 0; i < 12; i++) {
            sum += (d.charAt(i) - '0') * (i % 2 == 0 ? 1 : 3);
        }
        return (10 - sum % 10) % 10 == d.charAt(12) - '0';
    }

    /** No area 000, 666 or 900 and above, no group 00, no serial 0000: never issued. */
    private static boolean ssn(final Matcher m) {
        final int area = Integer.parseInt(m.group(1));
        return area != 0
                && area != 666
                && area < 900
                && Integer.parseInt(m.group(2)) != 0
                && Integer.parseInt(m.group(3)) != 0;
    }

    /** A domain of labels with a top-level name of letters, and none reserved for documentation. */
    private static boolean mailDomain(final String domain) {
        final String d = domain.endsWith(".") ? domain.substring(0, domain.length() - 1) : domain;
        final int dot = d.lastIndexOf('.');
        if (dot <= 0 || d.contains("..") || d.startsWith(".") || d.startsWith("-")) {
            return false;
        }
        final String tld = d.substring(dot + 1);
        return tld.length() >= 2
                && tld.length() <= 24
                && tld.chars().allMatch(Character::isLetter)
                && !RESERVED_DOMAIN.matcher(d).find();
    }

    /** Not private, loopback, link-local, shared, multicast or reserved for documentation. */
    private static boolean publicAddress(final Matcher m) {
        final int[] o = new int[4];
        for (int i = 0; i < 4; i++) {
            o[i] = Integer.parseInt(m.group(i + 1));
            if (o[i] > 255) {
                return false;
            }
        }
        return !(o[0] == 0
                || o[0] == 10
                || o[0] == 127
                || o[0] >= 224
                || (o[0] == 100 && o[1] >= 64 && o[1] <= 127)
                || (o[0] == 169 && o[1] == 254)
                || (o[0] == 172 && o[1] >= 16 && o[1] <= 31)
                || (o[0] == 192 && o[1] == 168)
                || (o[0] == 192 && o[1] == 0 && o[2] == 2)
                || (o[0] == 198 && (o[1] == 18 || o[1] == 19))
                || (o[0] == 198 && o[1] == 51 && o[2] == 100)
                || (o[0] == 203 && o[1] == 0 && o[2] == 113));
    }
}
