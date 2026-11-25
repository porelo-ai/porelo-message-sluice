package org.fuin.porelo.msgsluice.filter.detect;

import java.util.Objects;

/**
 * One piece of text handed to a detector.
 *
 * @param location where it sits in the message, for example {@code parts[0].text} or {@code
 *     metadata.note}
 * @param partIndex the index of its part, or -1 for text outside the parts
 * @param text the text itself; offsets in a finding count UTF-16 units into exactly this string
 * @param kind whether it is prose or source code, which decides the detectors that see it
 */
public record TextUnderCheck(String location, int partIndex, String text, TextKind kind) {

    /** Checks that nothing is missing. */
    public TextUnderCheck {
        Objects.requireNonNull(location, "location");
        Objects.requireNonNull(text, "text");
        Objects.requireNonNull(kind, "kind");
    }

    /** A text of prose. */
    public TextUnderCheck(final String location, final int partIndex, final String text) {
        this(location, partIndex, text, TextKind.PROSE);
    }

    /** A text of source code, judged as code rather than as prose. */
    public static TextUnderCheck code(
            final String location, final int partIndex, final String text) {
        return new TextUnderCheck(location, partIndex, text, TextKind.CODE);
    }
}
