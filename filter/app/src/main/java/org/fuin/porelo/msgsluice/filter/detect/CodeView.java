package org.fuin.porelo.msgsluice.filter.detect;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Source code as the program it compiles to holds its strings: string literals joined by {@code +}
 * or written side by side, on one line or over lines, read as the one string they make. A payload
 * cut into literals shorter than every threshold is one run again here, so the code detectors judge
 * it whole. Every character still points into the original text, so a finding made on the view is
 * redacted and reported where it was written.
 */
public final class CodeView {

    /** Whitespace and comments, which may stand between two joined literals. */
    private static final String GAP = "(?:\\s++|/\\*(?:[^*]|\\*(?!/))*+\\*/|//[^\\n]*+)*+";

    /**
     * The end of one literal and the start of the next: a quote not escaped and not opening an
     * empty literal, perhaps a parenthesis, whitespace or comments, a {@code +} or nothing, again,
     * perhaps a parenthesis, and the same quote.
     */
    private static final Pattern JOIN =
            Pattern.compile("(?<!\\\\)([\"'`])(?!\\1)\\)?+" + GAP + "(?:\\+" + GAP + ")?+\\(?+\\1");

    private CodeView() {}

    /**
     * The view of a normalized code text, or the text itself when no literals are joined in it.
     *
     * @param normalized the code text after the frame's normalization
     */
    public static Normalizer.Normalized of(final Normalizer.Normalized normalized) {
        final String text = normalized.text();
        final Matcher m = JOIN.matcher(text);
        if (!m.find()) {
            return normalized;
        }
        final StringBuilder out = new StringBuilder(text.length());
        final int[] from = new int[text.length()];
        final int[] to = new int[text.length()];
        int kept = 0;
        int at = 0;
        do {
            for (int i = at; i < m.start(); i++) {
                out.append(text.charAt(i));
                from[kept] = normalized.from()[i];
                to[kept] = normalized.to()[i];
                kept++;
            }
            at = m.end();
        } while (m.find());
        for (int i = at; i < text.length(); i++) {
            out.append(text.charAt(i));
            from[kept] = normalized.from()[i];
            to[kept] = normalized.to()[i];
            kept++;
        }
        return new Normalizer.Normalized(
                normalized.original(),
                out.toString(),
                java.util.Arrays.copyOf(from, kept),
                java.util.Arrays.copyOf(to, kept),
                List.of());
    }
}
