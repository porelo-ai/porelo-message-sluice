package org.fuin.porelo.msgsluice.filter.a2a;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * A comment's text is written as SARIF reads a message, so no part of it becomes a link or a
 * placeholder: a backslash only before a backslash or a square bracket, every square bracket after
 * one, every brace doubled. Read left to right, each escape taking the character after it.
 */
class ReviewMessageEscapingTest {

    @ParameterizedTest
    @ValueSource(
            strings = {
                "plain words",
                "a\\\\b",
                "\\\\",
                "\\[0\\]",
                "\\\\\\[",
                "{{0}}",
                "}}",
                "x \\] y",
                "an emoji 😀 stays"
            })
    void aTextEscapedAsSarifReadsItIsAccepted(final String written) {
        assertThat(ReviewProfileCheck.escaped(written)).as(written).isTrue();
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "a\\", "\\n", "\\{", "\\\\[", "[", "]", "{", "}", "{}", "{{{", "}}}", "\\[0]"
            })
    void aTextSarifWouldReadAsMarkupIsRefused(final String written) {
        assertThat(ReviewProfileCheck.escaped(written)).as(written).isFalse();
    }
}
