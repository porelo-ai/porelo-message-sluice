package org.fuin.porelo.msgsluice.filter;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import org.fuin.porelo.msgsluice.filter.WorkflowSteps.Step;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * A Maven cache keyed by the poms alone is hit exactly while the parent snapshot and its BOMs move,
 * and an exact hit is never saved over, so every run fetches them again from Maven Central. Each
 * run therefore restores under a key of its own and saves only what it fetched.
 */
class WorkflowMavenCacheTest {

    private static final Path WORKFLOWS = Path.of("..", "..", ".github", "workflows");

    private static final String SAVE = "./.github/actions/maven-cache-save";

    @ParameterizedTest
    @ValueSource(strings = {"build.yml", "shared-rules.yml"})
    void eachRunRestoresUnderItsOwnKeyAndSavesWhatItFetched(final String workflow)
            throws IOException {
        final List<Step> steps = WorkflowSteps.steps(WORKFLOWS.resolve(workflow));

        final Step restore =
                WorkflowSteps.step(WORKFLOWS.resolve(workflow), "Restore the Maven repository");
        assertThat(restore.keys().get("uses"))
                .as("restored only, so the job's end decides whether to save")
                .startsWith("actions/cache/restore@");
        assertThat(restore.keys().get("id"))
                .as("the id the save reads its key from")
                .isEqualTo("maven-cache");
        assertThat(restore.with().get("key"))
                .as("a key no earlier run holds, so this run can save under it")
                .contains("${{ github.run_id }}")
                .contains("${{ github.run_attempt }}");
        assertThat(steps)
                .as("no cache step that saves only when its key was missed")
                .noneMatch(
                        s ->
                                s.keys().getOrDefault("uses", "").startsWith("actions/cache@")
                                        && "~/.m2/repository".equals(s.with().get("path")));

        final Step last = steps.get(steps.size() - 1);
        assertThat(last.keys().get("uses")).as("the job's last step saves").isEqualTo(SAVE);
        assertThat(last.keys().get("if")).as("even after a failure").isEqualTo("always()");
        assertThat(last.with().get("key"))
                .as("under the key the job restored with")
                .isEqualTo("${{ steps.maven-cache.outputs.cache-primary-key }}");
    }
}
