package org.fuin.porelo.msgsluice.filter.detect;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import org.fuin.porelo.msgsluice.filter.decision.Finding;
import org.fuin.porelo.msgsluice.filter.decision.Redactor;
import org.fuin.porelo.msgsluice.filter.decision.Stage;

/**
 * Runs the detectors for each text's kind over it - those for prose or those for code - and turns
 * what they report into findings that may leave the house. Fails closed: a detector that throws, or
 * answers outside its contract, produces a finding that refuses.
 */
public final class DetectorRunner {

    public static final String DETECTOR_FAILED = "FRAME/DETECTOR_FAILED";

    public static final String UNREDACTABLE = "FRAME/UNREDACTABLE_FINDING";

    private static final Pattern RULE_ID = Pattern.compile("[A-Z0-9_]{1,32}/[A-Z0-9_]{1,48}");

    /** A reason quoting this much of the match in a row counts as quoting it. */
    private static final int QUOTE_WINDOW = 8;

    private final List<ContentDetector> detectors;

    private final List<ContentDetector> codeDetectors;

    private final Redactor redactor;

    /**
     * Judges prose and code alike with the same detectors: stricter for code than {@link
     * #DetectorRunner(List, List, Redactor)}, never more lenient.
     */
    public DetectorRunner(final List<ContentDetector> detectors, final Redactor redactor) {
        this(detectors, detectors, redactor);
    }

    /**
     * @param detectors those that judge a {@link TextKind#PROSE} text
     * @param codeDetectors those that judge a {@link TextKind#CODE} text
     */
    public DetectorRunner(
            final List<ContentDetector> detectors,
            final List<ContentDetector> codeDetectors,
            final Redactor redactor) {
        this.detectors = List.copyOf(detectors);
        this.codeDetectors = List.copyOf(codeDetectors);
        this.redactor = Objects.requireNonNull(redactor, "redactor");
    }

    /** The detectors that judge prose. */
    public List<ContentDetector> detectors() {
        return detectors;
    }

    /** The detectors that judge source code. */
    public List<ContentDetector> codeDetectors() {
        return codeDetectors;
    }

    /** Normalizes each text and runs the detectors for its kind over it. */
    public List<Finding> run(final List<TextUnderCheck> texts) {
        final List<Finding> findings = new ArrayList<>();
        for (final TextUnderCheck original : texts) {
            final Normalizer.Normalized normalized = Normalizer.normalize(original.text());
            for (final DetectorFinding found : normalized.findings()) {
                // The normalizer's own findings already point into the original.
                findings.add(
                        toFinding(
                                Normalizer.ID,
                                Normalizer.RULE_IDS,
                                Set.of(Normalizer.CATEGORY),
                                original,
                                found,
                                found.start(),
                                found.end(),
                                found.start(),
                                found.end(),
                                original.text()));
            }
            final List<Finding> own = judge(original, normalized);
            findings.addAll(own);
            if (original.kind() == TextKind.CODE) {
                final List<Finding> seen = new ArrayList<>(own);
                for (final Normalizer.Normalized view : CodeView.views(normalized)) {
                    for (final Finding f : judge(original, view)) {
                        if (seen.stream().noneMatch(o -> sameMatch(o, f))) {
                            findings.add(f);
                            seen.add(f);
                        }
                    }
                }
            }
        }
        return findings;
    }

    /**
     * The detectors for the text's kind over one reading of it, each finding mapped back into the
     * original.
     */
    private List<Finding> judge(
            final TextUnderCheck original, final Normalizer.Normalized normalized) {
        final List<Finding> findings = new ArrayList<>();
        final TextUnderCheck text =
                new TextUnderCheck(
                        original.location(),
                        original.partIndex(),
                        normalized.text(),
                        original.kind());
        for (final ContentDetector detector :
                text.kind() == TextKind.CODE ? codeDetectors : detectors) {
            final List<DetectorFinding> reported;
            try {
                reported = detector.inspect(text);
            } catch (final RuntimeException | StackOverflowError | OutOfMemoryError ex) {
                // The exception's message may quote the text, so only its type is named.
                findings.add(
                        Finding.frame(
                                DETECTOR_FAILED,
                                text.location(),
                                text.partIndex(),
                                "The check could not be completed ("
                                        + ex.getClass().getSimpleName()
                                        + "), so the message is not accepted.",
                                detector.id()));
                continue;
            }
            if (reported == null) {
                findings.add(
                        Finding.frame(
                                DETECTOR_FAILED,
                                text.location(),
                                text.partIndex(),
                                "The check answered outside its contract, so the message is not accepted.",
                                detector.id()));
                continue;
            }
            for (final DetectorFinding found : reported) {
                final int[] span = normalized.toOriginal(found.start(), found.end());
                findings.add(
                        toFinding(
                                detector.id(),
                                detector.ruleIds(),
                                detector.categories(),
                                original,
                                found,
                                span[0],
                                span[1],
                                found.start(),
                                found.end(),
                                normalized.text()));
            }
        }
        return findings;
    }

    /**
     * Whether the text's own finding already says what the view's does: the same rule over a span
     * that holds the view's, or the same failure of the same detector.
     */
    private static boolean sameMatch(final Finding own, final Finding view) {
        if (!own.ruleId().equals(view.ruleId())) {
            return false;
        }
        if (view.start() < 0) {
            return own.detector().equals(view.detector());
        }
        return own.start() <= view.start() && view.end() <= own.end();
    }

    /**
     * @param start the match in the original text; what is redacted and reported
     * @param normalizedText the text the detector saw, and {@code nStart}/{@code nEnd} the match in
     *     it; an explanation may quote neither form
     */
    private Finding toFinding(
            final String detectorId,
            final Set<String> ruleIds,
            final Set<String> categories,
            final TextUnderCheck text,
            final DetectorFinding f,
            final int start,
            final int end,
            final int nStart,
            final int nEnd,
            final String normalizedText) {
        if (!RULE_ID.matcher(f.ruleId()).matches()
                || !RULE_ID.matcher("X/" + f.category()).matches()
                || f.stage().alwaysRefuses()
                || !ruleIds.contains(f.ruleId())
                || !categories.contains(f.category())) {
            return Finding.frame(
                    DETECTOR_FAILED,
                    text.location(),
                    text.partIndex(),
                    "The check answered outside its contract, so the message is not accepted.",
                    detectorId);
        }
        final String excerpt;
        try {
            if (nStart < 0 || nEnd > normalizedText.length() || nStart >= nEnd) {
                throw new IllegalArgumentException("offsets outside the text");
            }
            excerpt = redactor.redact(text.text(), start, end, f.category());
        } catch (final IllegalArgumentException ex) {
            return Finding.frame(
                    UNREDACTABLE,
                    text.location(),
                    text.partIndex(),
                    "A finding could not be redacted, so the message is not accepted.",
                    detectorId);
        }
        if (quotes(f.reason(), text.text().substring(start, end))
                || quotes(f.reason(), normalizedText.substring(nStart, nEnd))) {
            return Finding.frame(
                    UNREDACTABLE,
                    text.location(),
                    text.partIndex(),
                    "A finding's explanation quoted what it matched, so the message is not accepted.",
                    detectorId);
        }
        return new Finding(
                f.ruleId(),
                f.stage() == Stage.CONTEXT ? Stage.CONTEXT : Stage.TEXT,
                f.severity(),
                f.category(),
                text.location(),
                text.partIndex(),
                start,
                end,
                excerpt,
                f.reason(),
                List.of(),
                detectorId);
    }

    /**
     * Whether the explanation repeats a piece of the match that could be a secret: eight characters
     * in a row of any word in it that is not a plain word - one with a digit, a symbol, or a
     * capital after a lower-case letter. A plain word like "characters" appears in an explanation
     * and in the prose around a payload alike.
     */
    private static boolean quotes(final String reason, final String matched) {
        for (final String word : matched.split("\\s+")) {
            if (word.length() < QUOTE_WINDOW || isPlainWord(word)) {
                continue;
            }
            for (int i = 0; i + QUOTE_WINDOW <= word.length(); i++) {
                if (reason.contains(word.substring(i, i + QUOTE_WINDOW))) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isPlainWord(final String word) {
        for (int i = 0; i < word.length(); i++) {
            final char c = word.charAt(i);
            if (!Character.isLetter(c)
                    || i > 0
                            && Character.isUpperCase(c)
                            && Character.isLowerCase(word.charAt(i - 1))) {
                return false;
            }
        }
        return true;
    }
}
