package org.fuin.porelo.msgsluice.filter;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * The sentence in {@code AGENTS.md} that says which Maven modules this repository has is held to
 * the modules that exist: the root's reactor and the directories beside it that carry a {@code
 * pom.xml}. A pointer check sees only what a document names, so a module added without a word, or
 * one named after it is gone, would stay unseen. Each module named also has its page in {@code
 * doc/}. Submodules are {@code check-readmes}' to hold, not this test's.
 */
@Tag("documents")
class ModulesDescribedTest {

    private static final Path ROOT = Path.of("..", "..");

    /** The section that describes the repository, up to the next heading. */
    private static final Pattern SECTION =
            Pattern.compile("(?ms)^## What this repository is\\s*$(.*?)(?=^## |\\z)");

    /** "in one Maven module", "in two Maven modules". */
    private static final Pattern COUNT = Pattern.compile("\\b(\\w+) Maven modules?\\b");

    /** A module is named as its directory: {@code `filter/`}. */
    private static final Pattern NAMED = Pattern.compile("`([\\w-]+)/`");

    private static final Pattern MODULE = Pattern.compile("<module>\\s*([^<\\s]+)\\s*</module>");

    private static final Map<String, Integer> NUMBERS =
            Map.of("one", 1, "two", 2, "three", 3, "four", 4, "five", 5, "six", 6);

    @Test
    void theModulesTheDescriptionNamesAreTheModulesThatExist() throws IOException {
        final String paragraph = firstParagraph(Files.readString(ROOT.resolve("AGENTS.md")));
        final Set<String> named = all(NAMED, paragraph);
        final Set<String> reactor = all(MODULE, Files.readString(ROOT.resolve("pom.xml")));
        final Set<String> directories = directoriesWithAPom();

        // A sentence or a reactor that cannot be read would leave three empty sets agreeing.
        assertThat(named).as("modules named in AGENTS.md: %s", paragraph).isNotEmpty();
        assertThat(reactor).as("modules in the root pom.xml").isNotEmpty();
        assertThat(count(paragraph))
                .as(
                        "the number of modules the sentence says, against those it names: %s",
                        paragraph)
                .isEqualTo(named.size());
        assertThat(reactor)
                .as("`AGENTS.md` names %s; `pom.xml` has %s", named, reactor)
                .isEqualTo(named);
        assertThat(directories)
                .as(
                        "`AGENTS.md` names %s; the directories with a pom.xml are %s",
                        named, directories)
                .isEqualTo(named);
        assertThat(named.stream().filter(m -> !Files.isRegularFile(page(m))).toList())
                .as("modules named in AGENTS.md without their page doc/<module>.md")
                .isEmpty();
    }

    private static String firstParagraph(final String agents) {
        final Matcher section = SECTION.matcher(agents);
        assertThat(section.find()).as("AGENTS.md has a section 'What this repository is'").isTrue();
        return section.group(1).strip().split("\\R\\s*\\R", 2)[0];
    }

    private static int count(final String paragraph) {
        final Matcher m = COUNT.matcher(paragraph);
        assertThat(m.find()).as("the sentence says how many Maven modules: %s", paragraph).isTrue();
        final Integer n = NUMBERS.get(m.group(1).toLowerCase(Locale.ROOT));
        assertThat(n).as("'%s' is a number written as a word", m.group(1)).isNotNull();
        return n;
    }

    private static Set<String> all(final Pattern pattern, final String text) {
        final Set<String> found = new TreeSet<>();
        final Matcher m = pattern.matcher(text);
        while (m.find()) {
            found.add(m.group(1));
        }
        return found;
    }

    private static Set<String> directoriesWithAPom() throws IOException {
        try (Stream<Path> children = Files.list(ROOT)) {
            final List<String> found =
                    children.filter(d -> Files.isRegularFile(d.resolve("pom.xml")))
                            .map(d -> d.getFileName().toString())
                            .toList();
            return new TreeSet<>(found);
        }
    }

    private static Path page(final String module) {
        return ROOT.resolve("doc").resolve(module + ".md");
    }
}
