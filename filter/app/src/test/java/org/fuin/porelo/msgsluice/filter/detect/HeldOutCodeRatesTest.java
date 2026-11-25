package org.fuin.porelo.msgsluice.filter.detect;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * The code detectors' false positives on code that played no part in setting their thresholds, per
 * language a suggested change is written in, and their share of refusals on a lockfile's checksum
 * lines. Harm guarded: a threshold that holds only on the corpus it was tuned on, refusing ordinary
 * suggestions in the other languages Porelo's repositories are written in.
 *
 * <p>The code is {@code code-corpus-other.txt}, frozen once from the committed {@code main} of
 * other Porelo repositories - Java from {@code porelo} and {@code porelo-planner}, Dart from {@code
 * porelo-frontend}, YAML and shell (every {@code .sh} file and every file without an extension
 * whose first line names a shell) from {@code porelo-host} and {@code porelo-buildtools} - cut as
 * {@link CodeRatesTest} describes: blocks at blank lines, snippets of 1 to 12 lines with a seed
 * taken from the repository and path, tabs as four spaces, dedented, those with fewer than 20
 * visible characters dropped, every second one kept, duplicates dropped, then spread evenly over
 * all candidates down to a few hundred per language. Left out are the changelog entries (prose in
 * YAML), every test file that holds a PEM header, a real-looking provider token, an Ansible vault,
 * an SSH public key or a quoted literal of 32 or more hex or 40 or more base64 characters (keys,
 * signatures, commit ids and hashes in test data), and every snippet naming an issue or a
 * machine-specific path. A line {@code #lang <name>} on its own starts each language's group; the
 * last group, {@code pubspec.lock}, is every {@code sha256} line of porelo-frontend's lockfile.
 */
class HeldOutCodeRatesTest {

    /** The frozen groups' sizes: a different count means the reference set itself changed. */
    static final Map<String, Integer> GROUP_SIZES =
            Map.of("java", 791, "dart", 445, "yaml", 299, "shell", 277, "pubspec.lock", 109);

    /** Refusals measured per language, asserted exactly, so the document's table stays true. */
    static final Map<String, Integer> REFUSED_MEASURED =
            Map.of("java", 2, "dart", 1, "yaml", 10, "shell", 3);

    /** The lockfile lines refused, measured: they hold hex or base64 checksums. */
    static final int LOCKFILE_REFUSED_MEASURED = 109;

    /** The held-out corpus, its groups in file order, each snippet stripped. */
    static Map<String, List<String>> groups() throws Exception {
        final String text;
        try (InputStream in =
                HeldOutCodeRatesTest.class.getResourceAsStream("/code-corpus-other.txt")) {
            assertThat(in).as("code-corpus-other.txt on the test class path").isNotNull();
            text = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        final Map<String, List<String>> groups = new LinkedHashMap<>();
        List<String> current = null;
        for (final String block : text.split("\n\n")) {
            final String snippet = block.strip();
            if (snippet.startsWith("#lang ")) {
                current = new ArrayList<>();
                groups.put(snippet.substring("#lang ".length()), current);
            } else if (!snippet.isEmpty()) {
                assertThat(current).as("a snippet before the first #lang line").isNotNull();
                current.add(snippet);
            }
        }
        final Map<String, Integer> sizes = new LinkedHashMap<>();
        groups.forEach((lang, snippets) -> sizes.put(lang, snippets.size()));
        assertThat(sizes).as("held-out corpus groups and sizes").isEqualTo(GROUP_SIZES);
        return groups;
    }

    /** Each language's snippets are refused exactly as often as measured. */
    @Test
    void codeInEveryLanguageWrittenOutsideThisRepositoryIsAccepted() throws Exception {
        final Map<String, List<String>> groups = groups();
        for (final Map.Entry<String, Integer> measured : REFUSED_MEASURED.entrySet()) {
            final List<String> code = groups.get(measured.getKey());
            final List<String> refused =
                    code.stream().filter(t -> !CodeRatesTest.judge(t).accepted()).toList();
            System.out.printf(
                    "%-6s %d snippets, %d refused%n",
                    measured.getKey(), code.size(), refused.size());
            assertThat(refused.size())
                    .as("%s false positives, %s", measured.getKey(), refused)
                    .isEqualTo(measured.getValue());
        }
    }

    /** Every checksum line of a lockfile is refused, as hex or base64. */
    @Test
    void aLockfilesChecksumLinesAreRefusedAsEncodedData() throws Exception {
        final List<String> lines = groups().get("pubspec.lock");
        final long refused = lines.stream().filter(t -> !CodeRatesTest.judge(t).accepted()).count();
        System.out.printf("pubspec.lock %d lines, %d refused%n", lines.size(), refused);
        assertThat(refused)
                .as("checksum lines refused of %d", lines.size())
                .isEqualTo(lines.size())
                .isEqualTo(LOCKFILE_REFUSED_MEASURED);
    }
}
