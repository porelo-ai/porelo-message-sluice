package org.fuin.porelo.msgsluice.filter.detect;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Base64;
import java.util.List;
import java.util.Random;
import org.fuin.porelo.msgsluice.filter.decision.Finding;
import org.junit.jupiter.api.Test;

/**
 * String literals joined by {@code +} or written side by side are judged as the one string they
 * make. Harm guarded: a payload cut into literals shorter than every threshold passing the code
 * detectors, and a finding on the joined string pointing anywhere but where it was written.
 */
class CodeViewTest {

    static final String BASE64 =
            Base64.getEncoder().encodeToString(Payloads.bytes(new Random(11), 48)).substring(0, 60);

    static List<Finding> judge(final String code) {
        return CodeRatesTest.RUNNER.run(List.of(TextUnderCheck.code("parts[0].text", 0, code)));
    }

    /** The view joins the literals and keeps every character's place in the original. */
    @Test
    void joinedLiteralsReadAsOneStringAndPointIntoTheOriginal() {
        final String code = "x = \"ab\" + \"cd\"\n    + 'ef' 'gh';";
        final Normalizer.Normalized view = CodeView.of(Normalizer.normalize(code));

        assertThat(view.text()).as("the joined view").isEqualTo("x = \"abcd\"\n    + 'efgh';");
        final int at = view.text().indexOf("cd");
        assertThat(view.toOriginal(at, at + 2))
                .as("where the view's cd was written")
                .containsExactly(code.indexOf("cd"), code.indexOf("cd") + 2);
    }

    /** A payload in four literals of 15 characters, over four lines, is refused and not shown. */
    @Test
    void aPayloadCutIntoShortLiteralsOverLinesIsRefusedAndNotRepeated() {
        final StringBuilder code = new StringBuilder("final String data =\n");
        for (int i = 0; i < 4; i++) {
            code.append(i == 0 ? "        \"" : "        + \"")
                    .append(BASE64, i * 15, i * 15 + 15)
                    .append("\"\n");
        }
        code.append(";");

        final List<Finding> findings = judge(code.toString());

        assertThat(findings)
                .as("rules found in %s", code)
                .anyMatch(f -> f.ruleId().startsWith("ENCODING/"));
        assertThat(findings)
                .as("no excerpt or reason repeats a piece")
                .allSatisfy(
                        f -> {
                            for (int i = 0; i < 4; i++) {
                                final String piece = BASE64.substring(i * 15 + 2, i * 15 + 12);
                                assertThat(String.valueOf(f.redactedExcerpt()))
                                        .as("excerpt")
                                        .doesNotContain(piece);
                                assertThat(f.reason()).as("reason").doesNotContain(piece);
                            }
                        });
    }

    /** Literals that are separate arguments, or joined with something between them, stay apart. */
    @Test
    void literalsNotJoinedAreNotReadAsOne() {
        final String code =
                "log(\""
                        + BASE64.substring(0, 20)
                        + "\", \""
                        + BASE64.substring(20, 40)
                        + "\"); y = \""
                        + BASE64.substring(40, 60)
                        + "\" + name + \"x\";";

        assertThat(CodeView.of(Normalizer.normalize(code)).text())
                .as("nothing here is joined")
                .isEqualTo(code);
    }

    /** Comments and parentheses between two literals, and backticks, still join them. */
    @Test
    void commentsParenthesesAndBackticksBetweenLiteralsStillJoin() {
        final String code = "x = (\"ab\") /* c */ + (\"cd\") +\n// d\n\"ef\"; y = `gh` + `ij`;";

        assertThat(CodeView.of(Normalizer.normalize(code)).text())
                .as("the joined view")
                .isEqualTo("x = (\"abcdef\"; y = `ghij`;");
    }

    /** An empty literal is no join, so code with one has no view of its own. */
    @Test
    void anEmptyLiteralIsNoJoin() {
        final Normalizer.Normalized normalized = Normalizer.normalize("x = \"\"; y = '';");

        assertThat(CodeView.of(normalized))
                .as("the view of code with empty literals")
                .isSameAs(normalized);
    }

    /** A finding on the view covers in the original everything it joined, separators included. */
    @Test
    void aViewFindingCoversTheSeparatorsItCrossed() {
        final String code =
                "d = \""
                        + BASE64.substring(0, 20)
                        + "\" + \""
                        + BASE64.substring(20, 40)
                        + "\" + \""
                        + BASE64.substring(40, 60)
                        + "\";";

        assertThat(judge(code))
                .as("a finding over the joined payload")
                .anySatisfy(
                        f -> {
                            assertThat(f.ruleId()).startsWith("ENCODING/");
                            assertThat(f.start())
                                    .as("start")
                                    .isLessThanOrEqualTo(code.indexOf(BASE64.substring(0, 4)));
                            assertThat(f.end())
                                    .as("end")
                                    .isGreaterThanOrEqualTo(
                                            code.indexOf(BASE64.substring(56, 60)) + 4);
                        });
    }

    /** 64 KB of joined one-letter literals is judged quickly. */
    @Test
    void sixtyFourKilobytesOfJoinedLiteralsAreJudgedQuickly() {
        final String code = "s = " + "\"a\" + ".repeat(64 * 1024 / 6) + "\"a\";";
        final long start = System.nanoTime();

        judge(code);

        assertThat((System.nanoTime() - start) / 1_000_000)
                .as("milliseconds to judge 64 KB of joined literals")
                .isLessThan(5_000);
    }

    /**
     * A payload found in one literal is reported once when the view finds it over the same span,
     * and again when the view's finding reaches further, so all of the joined payload is held.
     */
    @Test
    void aViewFindingIsKeptOnlyWhereItReachesFurther() {
        final String same = "a = \"" + BASE64 + "\" + \" tail\";";
        final String further = "a = \"" + BASE64 + "\" + \"tail\";";

        assertThat(judge(same))
                .as("findings of one rule over one place")
                .filteredOn(f -> f.ruleId().equals(EncodingSignatureDetector.BASE64))
                .hasSize(1);
        assertThat(judge(further))
                .as("the view's finding covers the joined tail too")
                .filteredOn(f -> f.ruleId().equals(EncodingSignatureDetector.BASE64))
                .anySatisfy(
                        f -> assertThat(f.end()).as("end").isGreaterThan(further.indexOf("tail")));
    }
}
