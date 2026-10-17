package org.fuin.porelo.msgsluice.filter;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.MalformedInputException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Tests that no tracked file of this repository says the product's old name, but where it must.
 * <p>
 * The repository was renamed by a script; a word it left behind, or one written later out of habit, names something
 * that is no longer there. The old name is spelled here in two pieces, so that this file does not say it.
 */
@Tag("documents")
class NoOldNameTest {

    private static final Path ROOT = Path.of("..", "..");

    private static final String OLD = "so" + "kar";

    /** The old name as a sentence writes it. */
    private static final String OLD_NAME = "So" + "kar";

    /**
     * Paths that keep the old name, name and text: the frozen corpus the encoding rates are measured on, and the
     * issue of the rename.
     */
    private static final Pattern KEPT_PATHS = Pattern.compile("filter/app/src/test/resources/english-corpus\\.txt|"
            + "issues/SL26-[^/]+\\.md"); // not-a-citation: the issue's own file

    /** Lines that keep it: the rename's own row in the issue index, and the changelog entry that says it. */
    private static final Pattern KEPT_LINES = Pattern.compile(".*<a id=\"sl26\"></a>.*"
            + "|- " + OLD_NAME + " is now Porelo: .*");

    /** The changelog is read up to its first released version; what was released stays as it was released. */
    private static final String CHANGELOG = "CHANGELOG.md";

    private static final Pattern RELEASED = Pattern.compile("## \\[\\d+\\.\\d+\\.\\d+].*");

    private static List<String> tracked() throws IOException, InterruptedException {
        final Process git = new ProcessBuilder("git", "ls-files", "-z").directory(ROOT.toFile())
                .redirectErrorStream(true).start();
        final String out = new String(git.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        assertThat(git.waitFor()).as("git ls-files: %s", out).isZero();
        return List.of(out.split("\0"));
    }

    private static List<String> lines(final String file) {
        final List<String> lines;
        try {
            lines = Files.readAllLines(ROOT.resolve(file));
        } catch (final MalformedInputException ex) {
            // Not text: nothing in it is read as a word.
            return List.of();
        } catch (final IOException ex) {
            throw new UncheckedIOException(ex);
        }
        if (!file.equals(CHANGELOG)) {
            return lines;
        }
        final List<String> unreleased = new ArrayList<>();
        for (final String line : lines) {
            if (RELEASED.matcher(line).matches()) {
                break;
            }
            unreleased.add(line);
        }
        return unreleased;
    }

    @Test
    void noTrackedFileSaysTheOldName() throws Exception {
        final List<String> files = tracked();
        final List<String> found = new ArrayList<>();
        for (final String file : files) {
            if (KEPT_PATHS.matcher(file).matches()) {
                continue;
            }
            if (file.toLowerCase(Locale.ROOT).contains(OLD)) {
                found.add(file + ": its path");
            }
            final List<String> lines = lines(file);
            for (int i = 0; i < lines.size(); i++) {
                final String line = lines.get(i);
                if (!KEPT_LINES.matcher(line).matches() && line.toLowerCase(Locale.ROOT).contains(OLD)) {
                    found.add(file + ":" + (i + 1) + ": " + line.strip());
                }
            }
        }

        // A guard that reads nothing passes forever: assert what it read, then what it checks.
        assertThat(files).as("tracked files read").contains("pom.xml", "AGENTS.md", CHANGELOG,
                "filter/app/src/test/resources/english-corpus.txt");
        assertThat(found).as("tracked files that say the old name").isEmpty();
    }

    @Test
    void theChangelogIsReadOnlyUpToItsFirstRelease() {
        // The released entries are kept by stopping at their heading; a heading the pattern missed would read them.
        assertThat(RELEASED.matcher("## [0.4.1] - 2026-10-08").matches()).as("a released heading").isTrue();
        assertThat(RELEASED.matcher("## [Unreleased]").matches()).as("the unreleased heading").isFalse();
        assertThat(lines(CHANGELOG)).as("the changelog's unreleased part").contains("## [Unreleased]")
                .noneMatch(l -> RELEASED.matcher(l).matches());
    }
}
