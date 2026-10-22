package org.fuin.porelo.msgsluice.filter.detect;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

import java.math.BigInteger;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;
import org.fuin.porelo.msgsluice.filter.decision.Finding;
import org.fuin.porelo.msgsluice.filter.decision.Verdict;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Personal and financial data in a message is found with its default severity, judged by the
 * default configuration, and its value never comes back in the answer. Numbers with a checksum are
 * computed here, so that the source holds no number a scanner would take for a real one.
 */
class PersonalDataTest {

    /** {@code prefix} completed by its Luhn check digit. */
    static String luhn(final String prefix) {
        int sum = 0;
        for (int i = 0; i < prefix.length(); i++) {
            int d = prefix.charAt(prefix.length() - 1 - i) - '0';
            if (i % 2 == 0) {
                d *= 2;
                if (d > 9) {
                    d -= 9;
                }
            }
            sum += d;
        }
        return prefix + (10 - sum % 10) % 10;
    }

    /** An IBAN of {@code country} and {@code bban} with its MOD-97 check digits. */
    static String iban(final String country, final String bban) {
        final StringBuilder digits = new StringBuilder();
        for (final char c : (bban + country + "00").toCharArray()) {
            digits.append(Character.isLetter(c) ? String.valueOf(c - 'A' + 10) : String.valueOf(c));
        }
        final int check =
                98 - new BigInteger(digits.toString()).mod(BigInteger.valueOf(97)).intValue();
        return country + String.format("%02d", check) + bban;
    }

    /** A Swiss social insurance number, 756 and nine digits completed by its EAN-13 check digit. */
    static String ahv(final String nine) {
        final String twelve = "756" + nine;
        int sum = 0;
        for (int i = 0; i < 12; i++) {
            sum += (twelve.charAt(i) - '0') * (i % 2 == 0 ? 1 : 3);
        }
        final String all = twelve + (10 - sum % 10) % 10;
        return all.substring(0, 3)
                + "."
                + all.substring(3, 7)
                + "."
                + all.substring(7, 11)
                + "."
                + all.substring(11);
    }

    static String grouped(final String digits) {
        return digits.replaceAll("(\\d{4})(?=\\d)", "$1 ");
    }

    /** The category, its default severity, the value that must not come back, and the sentence. */
    static Stream<Arguments> personalData() {
        final String visa = luhn("453914880343646");
        final String amex = luhn("37828224631000");
        final String ch = iban("CH", "0483501234567800" + "9");
        final String chLower = iban("CH", "00762011" + "623852957").toLowerCase(Locale.ROOT);
        final String de = iban("DE", "37040044" + "0532013001");
        final String at = iban("AT", "19043002" + "34573201");
        final String ahv = ahv("123456789");
        final String ahvPlain = ahv.replace(".", "");
        return Stream.of(
                Arguments.of("CREDIT_CARD", "BLOCKING", visa, "Charge card " + grouped(visa) + "."),
                Arguments.of(
                        "CREDIT_CARD",
                        "BLOCKING",
                        visa,
                        "Charge " + grouped(visa) + " 12/29 please"),
                Arguments.of(
                        "CREDIT_CARD",
                        "BLOCKING",
                        visa,
                        "Charge card " + grouped(visa).replace(' ', '.') + "."),
                Arguments.of(
                        "CREDIT_CARD", "BLOCKING", amex, "The Amex is " + amex + ", exp 09/29."),
                Arguments.of(
                        "IBAN",
                        "BLOCKING",
                        ch,
                        "Transfer it to " + ch.replaceAll("(.{4})(?=.)", "$1 ") + " today."),
                Arguments.of(
                        "IBAN",
                        "BLOCKING",
                        chLower,
                        "Transfer it to " + chLower.replaceAll("(.{4})(?=.)", "$1 ") + " today."),
                Arguments.of("IBAN", "BLOCKING", de, "Account: " + de),
                Arguments.of(
                        "IBAN",
                        "BLOCKING",
                        at,
                        "Account " + at.replaceAll("(.{4})(?=.)", "$1 ") + " 0000"),
                Arguments.of("NATIONAL_ID", "BLOCKING", ahv, "Her AHV number is " + ahv + "."),
                Arguments.of(
                        "NATIONAL_ID", "BLOCKING", ahvPlain, "Her AHV number is " + ahvPlain + "."),
                Arguments.of(
                        "NATIONAL_ID",
                        "BLOCKING",
                        "123-45-" + "6789",
                        "SSN: 123-45-" + "6789 on file."),
                Arguments.of(
                        "EMAIL",
                        "SUSPICIOUS",
                        "anna.meier",
                        "Write to anna.meier@firm.ch about it."),
                Arguments.of(
                        "EMAIL",
                        "SUSPICIOUS",
                        "jörg.müller@firm.ch",
                        "Write to jörg.müller@firm.ch about it."),
                Arguments.of("EMAIL", "SUSPICIOUS", "anna.meier", "Write anna.meier@firm.ch: now"),
                Arguments.of("PHONE", "SUSPICIOUS", "123 45 67", "Call her on +41 79 123 45 67."),
                Arguments.of("PHONE", "SUSPICIOUS", "555-0187", "My phone is (415) 555-0187."),
                Arguments.of(
                        "IP_ADDRESS", "SUSPICIOUS", "81.221.10.5", "The office is 81.221.10.5."),
                Arguments.of("IP_ADDRESS", "SUSPICIOUS", "81.221.10.5", "v 81.221.10.5"),
                Arguments.of(
                        "DATE_OF_BIRTH", "SUSPICIOUS", "12.03.1985", "He was born on 12.03.1985."),
                Arguments.of(
                        "DATE_OF_BIRTH",
                        "SUSPICIOUS",
                        "1985-03-12",
                        "Date of birth: 1985-03-12, as in the form."),
                Arguments.of(
                        "POSTAL_ADDRESS",
                        "INFO",
                        "Baker Street",
                        "Send it to 221 Baker Street, London."));
    }

    @ParameterizedTest(name = "{0}: {3}")
    @MethodSource("personalData")
    void personalDataIsFoundWithItsSeverityAndNotRepeated(
            final String category, final String severity, final String value, final String text)
            throws Exception {
        final Verdict verdict = CredentialsTest.judge(text, Map.of());
        assertThat(
                        verdict.findings().stream()
                                .filter(f -> f.category().equals(category))
                                .map(f -> f.severity().name())
                                .distinct()
                                .toList())
                .as("severity of %s in: %s", category, text)
                .containsExactly(severity);
        assertThat(verdict.findings().toString())
                .as("the value itself is in no finding")
                .doesNotContain(value);
        assertThat(
                        verdict.findings().stream()
                                .filter(f -> f.category().equals(category))
                                .map(f -> text.substring(f.start(), f.end()))
                                .toList())
                .as("finding covers the value in: %s", text)
                .anySatisfy(
                        m ->
                                assertThat(m.replaceAll("[ .]", ""))
                                        .as("matched text in: %s", text)
                                        .contains(value.replaceAll("[ .]", "")));
        if (severity.equals("BLOCKING")) {
            assertThat(verdict.accepted()).as("accepted: %s", text).isFalse();
        }
        assertThat(
                        CredentialsTest.judge(
                                        text,
                                        Map.of("decision.disabledRules", "PERSONAL/" + category))
                                .findings()
                                .stream()
                                .map(Finding::category))
                .as("with PERSONAL/%s switched off", category)
                .doesNotContain(category);
    }

    @Test
    void contactDataIsRefusedOnceAProjectSaysSo() throws Exception {
        final String text = "Write to anna.meier@firm.ch or call +41 79 123 45 67.";
        assertThat(
                        CredentialsTest.judge(text, Map.of("decision.severity.EMAIL", "BLOCKING"))
                                .accepted())
                .as("an e-mail address with EMAIL graded blocking")
                .isFalse();
        assertThat(
                        CredentialsTest.judge(text, Map.of("decision.severity.PHONE", "BLOCKING"))
                                .accepted())
                .as("a phone number with PHONE graded blocking")
                .isFalse();
    }

    @Test
    void numbersThatOnlyLookLikePersonalDataAreNot() throws Exception {
        final String visa = luhn("453914880343646");
        final String badCard =
                visa.substring(0, 15) + (char) ('0' + (visa.charAt(15) - '0' + 1) % 10);
        final String ch = iban("CH", "0483501234567800" + "9");
        final String badIban =
                ch.substring(0, 2) + (ch.charAt(2) == '9' ? "1" : "9") + ch.substring(3);
        for (final String text :
                List.of(
                        "The card " + badCard + " fails its checksum.",
                        "The account " + badIban + " fails its checksum.",
                        "Order " + ahv("123456789").replace(".", "") + "0 is no AHV number.",
                        "The VM is at 192.168.122.174, the host at 10.0.0.1 and 127.0.0.1.",
                        "Build 39730936077 published 0.5.1~snapshot.57 on 2026-10-22.",
                        "GraalVM 25.0.4 and Maven 3.9.15 are pinned.",
                        "Clone git@github.com:porelo-ai/porelo.git and run it.",
                        "Write to someone@example.com in the documentation.",
                        // Issue numbers before words, each of which passed MOD-97 by chance.
                        "PJ01 says only that both transports are named.", // not-a-citation
                        "With GH03 Finished and the rest open.", // not-a-citation
                        "It is CC36 Analyzed and sized.", // not-a-citation
                        "Read PJ54 provider contract first.", // not-a-citation
                        "The run took 05:56:55Z to 05:59:36Z, 2700 tests.",
                        "Released 2026-10-17; the next is due 2026-11-01.",
                        "The runner has Temurin 21.0.8.1 and GraalVM CE 25.0.4.1+1 installed.",
                        "Built from snapshot 181.2.0.3 of the parent.",
                        "Tagged v1.2.3.4 on the branch.",
                        "The resolvers 1.1.1.1, 8.8.8.8 and 9.9.9.9 are refused by the firewall.")) {
            assertThat(
                            CredentialsTest.judge(text, Map.of()).findings().stream()
                                    .filter(f -> f.ruleId().startsWith("PERSONAL/"))
                                    .filter(f -> !f.severity().name().equals("INFO"))
                                    .map(Finding::ruleId)
                                    .toList())
                    .as("personal-data findings in: %s", text)
                    .isEmpty();
        }
    }

    /** Text an adversary shapes so that a rule takes seconds or overflows the stack. */
    static Stream<Arguments> shapedToBeSlow() {
        return Stream.of(
                Arguments.of("digits and spaces", "1 ".repeat(32768)),
                Arguments.of(
                        "an address without an end", "a.".repeat(16384) + "@" + "b.".repeat(16383)),
                Arguments.of("'@' repeated", "a@".repeat(32768)),
                Arguments.of("a country and digits", "CH12 ".repeat(13107)), // not-a-citation
                Arguments.of("'+1 ' repeated", "+1 ".repeat(21845)),
                Arguments.of("'born ' repeated", "born ".repeat(13107)),
                Arguments.of("'phone ' repeated", "phone ".repeat(10922)),
                Arguments.of("'call me at ' repeated", "call me at ".repeat(5957)),
                Arguments.of("'11 ' repeated", "11 ".repeat(21845)),
                Arguments.of("'1 Aaa Bbb ' repeated", "1 Aaa Bbb ".repeat(6553)));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("shapedToBeSlow")
    void textShapedToBeSlowIsReadInLinearTime(final String name, final String text) {
        final PersonalDataDetector detector = new PersonalDataDetector(java.util.Set.of());
        // A message whose check hangs holds up every message behind it in the pass.
        assertTimeoutPreemptively(
                Duration.ofSeconds(2),
                () -> detector.inspect(new TextUnderCheck("parts[0].text", 0, text)),
                "64K characters of " + name + " read within two seconds");
    }
}
