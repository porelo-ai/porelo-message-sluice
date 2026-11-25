package org.fuin.porelo.msgsluice.filter.detect;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Source code as the program it compiles to holds its strings: string literals joined by {@code +}
 * or written side by side, on one line or over lines, read as the one string they make, and escapes
 * that write a character ({@code \\uXXXX}, {@code \\u{X}}, {@code \\UXXXXXXXX}, {@code \\xXX}) read
 * as that character. A payload cut into literals shorter than every threshold, or written as
 * escapes, is itself again here, so the code detectors judge it. Every character still points into
 * the original text, so a finding made on the view is redacted and reported where it was written.
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

    /**
     * An escape: one that writes a character by its number, read as that character, or any other,
     * an escaped backslash among them, consumed as it is so a backslash it holds escapes nothing.
     */
    private static final Pattern ESCAPE =
            Pattern.compile(
                    "\\\\(?:u++([0-9A-Fa-f]{4})|u\\{([0-9A-Fa-f]{1,6})\\}|U([0-9A-Fa-f]{8})"
                            + "|x([0-9A-Fa-f]{2})|[\\s\\S])");

    private CodeView() {}

    /**
     * The view of a normalized code text, or the text itself when nothing in it is joined or
     * escaped.
     *
     * @param normalized the code text after the frame's normalization
     */
    public static Normalizer.Normalized of(final Normalizer.Normalized normalized) {
        return rewrite(joined(normalized), ESCAPE, CodeView::character);
    }

    /**
     * The readings the code detectors judge besides the text: its literals joined, and those with
     * their escapes read, each only where it differs from the one before. Both are kept, as escapes
     * joined but not read are one long token, which the long-token check holds.
     *
     * @param normalized the code text after the frame's normalization
     */
    public static List<Normalizer.Normalized> views(final Normalizer.Normalized normalized) {
        final Normalizer.Normalized joined = joined(normalized);
        final Normalizer.Normalized read = rewrite(joined, ESCAPE, CodeView::character);
        final List<Normalizer.Normalized> views = new ArrayList<>();
        if (joined != normalized) {
            views.add(joined);
        }
        if (read != joined) {
            views.add(read);
        }
        return views;
    }

    private static Normalizer.Normalized joined(final Normalizer.Normalized normalized) {
        return rewrite(normalized, JOIN, m -> "");
    }

    /**
     * The character an escape writes, folded as the frame folds text, nothing for one that is
     * invisible, and the escape itself when it writes no character by number.
     */
    private static String character(final Matcher m) {
        String hex = null;
        for (int g = 1; g <= 4 && hex == null; g++) {
            hex = m.group(g);
        }
        if (hex == null) {
            return m.group();
        }
        final int codePoint = Integer.parseUnsignedInt(Objects.requireNonNull(hex), 16);
        if (!Character.isValidCodePoint(codePoint)) {
            return m.group();
        }
        if (invisible(codePoint)) {
            // A character nobody sees, between a payload's pieces, would cut its run.
            return "";
        }
        return java.text.Normalizer.normalize(
                new String(Character.toChars(codePoint)), java.text.Normalizer.Form.NFKC);
    }

    private static boolean invisible(final int codePoint) {
        final int type = Character.getType(codePoint);
        return type == Character.FORMAT
                || type == Character.CONTROL
                || type == Character.SURROGATE
                || type == Character.UNASSIGNED
                || type == Character.PRIVATE_USE
                || codePoint >= 0xFE00 && codePoint <= 0xFE0F;
    }

    /**
     * Every match replaced by what {@code replacement} gives, each character of a replacement
     * pointing at the whole match it came from.
     */
    private static Normalizer.Normalized rewrite(
            final Normalizer.Normalized in,
            final Pattern pattern,
            final Function<Matcher, String> replacement) {
        final String text = in.text();
        final Matcher m = pattern.matcher(text);
        if (!m.find()) {
            return in;
        }
        final StringBuilder out = new StringBuilder(text.length());
        int[] from = new int[text.length()];
        int[] to = new int[text.length()];
        int kept = 0;
        int at = 0;
        boolean changed = false;
        do {
            for (int i = at; i < m.start(); i++) {
                out.append(text.charAt(i));
                from[kept] = in.from()[i];
                to[kept] = in.to()[i];
                kept++;
            }
            final String with = replacement.apply(m);
            final boolean same = with.equals(m.group());
            changed |= !same;
            // Folding may write more characters than the escape held.
            if (kept + with.length() > from.length) {
                from = Arrays.copyOf(from, kept + with.length() + text.length());
                to = Arrays.copyOf(to, from.length);
            }
            for (int i = 0; i < with.length(); i++) {
                out.append(with.charAt(i));
                from[kept] = in.from()[same ? m.start() + i : m.start()];
                to[kept] = in.to()[same ? m.start() + i : m.end() - 1];
                kept++;
            }
            at = m.end();
        } while (m.find());
        if (!changed) {
            return in;
        }
        if (kept + text.length() - at > from.length) {
            from = Arrays.copyOf(from, kept + text.length() - at);
            to = Arrays.copyOf(to, from.length);
        }
        for (int i = at; i < text.length(); i++) {
            out.append(text.charAt(i));
            from[kept] = in.from()[i];
            to[kept] = in.to()[i];
            kept++;
        }
        return new Normalizer.Normalized(
                in.original(),
                out.toString(),
                Arrays.copyOf(from, kept),
                Arrays.copyOf(to, kept),
                List.of());
    }
}
