package org.fuin.porelo.msgsluice.filter;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * The Build checks the commit messages of every push, and the daily run those of main's last day,
 * so a direct push to {@code main} cannot pass the rule around the pull request's check. Each
 * step's own script runs against stubs of git and java. Read as lines, the way the workflows are
 * written: a step is a twelve-space list item, its keys fourteen spaces in, its environment and
 * script eighteen. Tagged, since a change to the workflows alone starts no Build.
 */
@Tag("documents")
class WorkflowCommitsTest {

    private static final Path WORKFLOWS = Path.of("..", "..", ".github", "workflows");
    private static final Path BUILD = WORKFLOWS.resolve("build.yml");
    private static final Path SHARED_RULES = WORKFLOWS.resolve("shared-rules.yml");
    private static final String PUSH_STEP = "The pushed commits' messages";
    private static final String BUILD_STEP = "Build, unit tests, native images and packages";
    private static final String PULL_REQUEST_STEP = "The pull request's commit messages";
    private static final String DAY_STEP = "Main's commits of the last day";
    private static final String CLASSPATH = "/stub/a.jar:/stub/b.jar";
    private static final String CHECKOUT = "checkout";
    private static final String TOOLS_STEP = "The pinned GraalVM";

    @TempDir Path directory;

    // Prevents the check from being skipped on a push or fed a range a workflow expression built.
    @Test
    void theBuildChecksThePushedRangeFromItsOwnEnvironment() throws IOException {
        final Step step = step(BUILD, PUSH_STEP);
        assertThat(step.keys().get("if"))
                .as("the step runs on a push")
                .isEqualTo("github.event_name == 'push'");
        assertThat(step.env().get("PUSH_BEFORE"))
                .as("the range starts where the push began")
                .isEqualTo("${{ github.event.before }}");
        assertThat(step.run())
                .as("the range reaches the script through its environment alone")
                .doesNotContain("${{");
    }

    // Prevents a range the checkout cannot read, and a check before the step that reads the tools.
    @Test
    void theBuildJobChecksOutTheWholeHistoryAndChecksAfterItsBuild() throws IOException {
        final List<Step> steps = steps(BUILD);
        final List<String> names = steps.stream().map(Step::name).toList();
        assertThat(names.stream().filter(CHECKOUT::equals).count())
                .as("the build job checks out once")
                .isEqualTo(1);
        assertThat(steps.get(names.indexOf(CHECKOUT)).with().get("fetch-depth"))
                .as("the checkout holds every commit of a pushed range")
                .isEqualTo("0");
        assertThat(names.indexOf(BUILD_STEP)).as("the build job builds").isNotNegative();
        assertThat(names.indexOf(TOOLS_STEP))
                .as("the build job resolves the tools' classpath")
                .isNotNegative();
        assertThat(names.indexOf(PUSH_STEP))
                .as("the check runs after the checkout, the tools' classpath and the build")
                .isGreaterThan(names.indexOf(BUILD_STEP))
                .isGreaterThan(names.indexOf(TOOLS_STEP))
                .isGreaterThan(names.indexOf(CHECKOUT));
    }

    // Prevents a pushed range from going unchecked, and a refusal from passing.
    @Test
    void aPushedRangeReachesCheckCommitsAndItsRefusalFailsTheStep()
            throws IOException, InterruptedException {
        final Run passed = push("refs/heads/main", "1111111", "1111111", 0);
        assertThat(passed.exit).as("a clean range: %s", passed.log).isZero();
        assertThat(passed.java)
                .as("check-commits reads exactly the pushed range, with the tools' classpath")
                .containsExactly(
                        "-cp",
                        CLASSPATH,
                        "org.fuin.porelo.release.Main",
                        "check-commits",
                        "1111111..2222222");

        final Run refused = push("refs/heads/main", "1111111", "1111111", 1);
        assertThat(refused.exit).as("a refused range fails the step: %s", refused.log).isNotZero();
    }

    // Prevents a new branch, whose start is all zeros, from checking nothing.
    @Test
    void aNewBranchIsCheckedFromMain() throws IOException, InterruptedException {
        final Run run = push("refs/heads/topic", "0000000000000000000000000000000000000000", "", 0);
        assertThat(run.exit).as("a new branch: %s", run.log).isZero();
        assertThat(run.java).as("its range starts at main").endsWith("origin/main..2222222");
    }

    // Prevents a branch pushed over, whose old tip the checkout lacks, from failing a clean push.
    @Test
    void aBranchPushedOverIsCheckedFromMain() throws IOException, InterruptedException {
        final Run run = push("refs/heads/topic", "3333333", "1111111", 0);
        assertThat(run.exit).as("a branch pushed over: %s", run.log).isZero();
        assertThat(run.java).as("its range starts at main").endsWith("origin/main..2222222");
    }

    // Prevents a tag, whose range from main is empty, from checking nothing.
    @Test
    void aTagChecksTheCommitItNames() throws IOException, InterruptedException {
        final Run run = push("refs/tags/v1.2.3", "0000000000000000000000000000000000000000", "", 0);
        assertThat(run.exit).as("a tag: %s", run.log).isZero();
        assertThat(run.java).as("its range is the commit it names").endsWith("2222222~1..2222222");
    }

    // Prevents main moved from a commit nobody holds from passing as an empty range.
    @Test
    void mainFromACommitTheCheckoutLacksFailsLoudly() throws IOException, InterruptedException {
        final Run run = push("refs/heads/main", "3333333", "1111111", 0);
        assertThat(run.exit).as("main from an unknown commit: %s", run.log).isNotZero();
        assertThat(run.log).as("the refusal is a visible workflow error").contains("::error::");
        assertThat(run.java).as("check-commits is not started for it").isEmpty();
    }

    // Prevents a missing start of the range from silently checking nothing.
    @Test
    void aMissingStartFailsLoudly() throws IOException, InterruptedException {
        final Run run = push("refs/heads/main", "", "1111111", 0);
        assertThat(run.exit).as("no start of the range: %s", run.log).isNotZero();
        assertThat(run.log).as("the refusal is a visible workflow error").contains("::error::");
        assertThat(run.java).as("check-commits is not started without a range").isEmpty();

        final Run branch = push("refs/heads/topic", "", "1111111", 0);
        assertThat(branch.exit).as("a branch without a start: %s", branch.log).isNotZero();
        assertThat(branch.java).as("a branch's range is not started at main either").isEmpty();
    }

    // Prevents a push the Build never starts from going unchecked: the schedule reads main's last
    // day.
    @Test
    void theScheduleChecksMainsLastDay() throws IOException, InterruptedException {
        final Step step = step(SHARED_RULES, DAY_STEP);
        assertThat(step.keys().get("if"))
                .as("the step runs on the schedule")
                .isEqualTo("github.event_name == 'schedule'");
        assertThat(step.run())
                .as("the range comes from git, not a workflow expression")
                .doesNotContain("${{");
        final List<Step> steps = steps(SHARED_RULES);
        assertThat(steps.get(0).name()).as("the job checks out first").isEqualTo(CHECKOUT);
        final List<String> names = steps.stream().map(Step::name).toList();
        assertThat(names.stream().filter(CHECKOUT::equals).count())
                .as("the job checks out once")
                .isEqualTo(1);
        assertThat(names.indexOf(TOOLS_STEP))
                .as("the job resolves the tools' classpath")
                .isNotNegative();
        assertThat(Math.min(names.indexOf(DAY_STEP), names.indexOf(PULL_REQUEST_STEP)))
                .as("both commit checks run after the tools' classpath is resolved")
                .isGreaterThan(names.indexOf(TOOLS_STEP));
        assertThat(steps.get(0).with().get("fetch-depth"))
                .as("the scheduled checkout holds main's last day")
                .isEqualTo("0");

        final Run run = execute(step, Map.of("DAY_START", "4444444"), 0);
        assertThat(run.exit).as("a clean day: %s", run.log).isZero();
        assertThat(run.java)
                .as("check-commits reads main's commits since the last day began")
                .containsExactly(
                        "-cp",
                        CLASSPATH,
                        "org.fuin.porelo.release.Main",
                        "check-commits",
                        "4444444..HEAD");
        assertThat(execute(step, Map.of("DAY_START", "4444444"), 1).exit)
                .as("a refused day fails the step")
                .isNotZero();
    }

    // Prevents a repository younger than a day from checking nothing, or leaving out its first
    // commit.
    @Test
    void aRepositoryYoungerThanADayIsReadWhole() throws IOException, InterruptedException {
        final Run run = execute(step(SHARED_RULES, DAY_STEP), Map.of("DAY_START", ""), 0);
        assertThat(run.exit).as("a young repository: %s", run.log).isZero();
        assertThat(run.java).as("it is read whole").endsWith("check-commits", "HEAD");
    }

    // Prevents a second fetch, which a private forge copy refuses to a checkout without
    // credentials.
    @Test
    void aMembersPullRequestIsCheckedFromTheCheckoutAlone()
            throws IOException, InterruptedException {
        final Run run = pullRequest("porelo-ai/porelo-message-sluice", "MEMBER", "someone");
        assertThat(run.exit).as("a member's pull request, nothing fetched: %s", run.log).isZero();
        assertThat(run.java)
                .as("its own commits are read, as a member's, with the tools' classpath")
                .containsExactly(
                        "-cp",
                        CLASSPATH,
                        "org.fuin.porelo.release.Main",
                        "check-commits",
                        "6666666..7777777");
        assertThat(pullRequest("porelo-ai/porelo-message-sluice", "MEMBER", "someone", 1).exit)
                .as("a refused pull request fails the step")
                .isNotZero();
    }

    // Prevents the pull request's facts from reaching the script any way but its environment, where
    // a workflow expression in the script would run what a pull request's title or branch carries.
    @Test
    void thePullRequestsFactsReachTheScriptThroughItsEnvironment() throws IOException {
        final Step step = step(SHARED_RULES, PULL_REQUEST_STEP);
        assertThat(step.keys().get("if"))
                .as("the step runs on a pull request")
                .isEqualTo("github.event_name == 'pull_request'");
        assertThat(step.env().get("PR_BASE"))
                .as("the range starts at the pull request's base")
                .isEqualTo("${{ github.event.pull_request.base.sha }}");
        assertThat(step.env().get("PR_HEAD"))
                .as("the range ends at the pull request's head")
                .isEqualTo("${{ github.event.pull_request.head.sha }}");
        assertThat(step.env().get("PR_AUTHOR"))
                .as("the author is the pull request's own")
                .isEqualTo("${{ github.event.pull_request.user.login }}");
        assertThat(step.env().get("PR_REPOSITORY"))
                .as("the repository is the pull request's head's")
                .isEqualTo("${{ github.event.pull_request.head.repo.full_name }}");
        assertThat(step.env().get("PR_ASSOCIATION"))
                .as("the association is the pull request's author's")
                .isEqualTo("${{ github.event.pull_request.author_association }}");
        assertThat(step.run()).as("the script reads its environment alone").doesNotContain("${{");
    }

    // Prevents the repository's own update workflow's pull request from being refused for want of a
    // sign-off, though GitHub names its author a contributor.
    @Test
    void theOwnWorkflowsPullRequestIsFromInside() throws IOException, InterruptedException {
        final Run run =
                pullRequest(
                        "porelo-ai/porelo-message-sluice", "CONTRIBUTOR", "github-actions[bot]");
        assertThat(run.exit).as("the own workflow's pull request: %s", run.log).isZero();
        assertThat(run.java).as("its commits need no sign-off").endsWith("6666666..7777777");
    }

    // Prevents code from outside from passing without its author's sign-off.
    @Test
    void aPullRequestFromElsewhereStaysFromOutside() throws IOException, InterruptedException {
        assertThat(
                        pullRequest(
                                        "someone/porelo-message-sluice",
                                        "CONTRIBUTOR",
                                        "github-actions[bot]")
                                .java)
                .as("the bot's pull request from another repository")
                .endsWith("6666666..7777777", "--outside");
        assertThat(pullRequest("porelo-ai/porelo-message-sluice", "CONTRIBUTOR", "someone").java)
                .as("a contributor's pull request from this repository")
                .endsWith("6666666..7777777", "--outside");
        assertThat(pullRequest("someone/porelo-message-sluice", "MEMBER", "someone").java)
                .as("a member's pull request from another repository")
                .endsWith("6666666..7777777", "--outside");
    }

    private Run pullRequest(final String repository, final String association, final String author)
            throws IOException, InterruptedException {
        return pullRequest(repository, association, author, 0);
    }

    private Run pullRequest(
            final String repository,
            final String association,
            final String author,
            final int checkExit)
            throws IOException, InterruptedException {
        return execute(
                step(SHARED_RULES, PULL_REQUEST_STEP),
                Map.of(
                        "PR_BASE",
                        "6666666",
                        "PR_HEAD",
                        "7777777",
                        "PR_REPOSITORY",
                        repository,
                        "GITHUB_REPOSITORY",
                        "porelo-ai/porelo-message-sluice",
                        "PR_ASSOCIATION",
                        association,
                        "PR_AUTHOR",
                        author),
                checkExit);
    }

    private record Run(int exit, String log, List<String> java) {}

    /**
     * Runs the Build's step for a push of {@code ref} from {@code before}; git holds {@code known}.
     */
    private Run push(final String ref, final String before, final String known, final int checkExit)
            throws IOException, InterruptedException {
        return execute(
                step(BUILD, PUSH_STEP),
                Map.of(
                        "PUSH_BEFORE",
                        before,
                        "GITHUB_SHA",
                        "2222222",
                        "GITHUB_REF",
                        ref,
                        "GITHUB_REF_TYPE",
                        ref.startsWith("refs/tags/") ? "tag" : "branch",
                        "KNOWN",
                        known.isEmpty() ? "none" : known),
                checkExit);
    }

    /** Runs a step's own script with git and java stubbed, in the given environment. */
    private Run execute(final Step step, final Map<String, String> environment, final int checkExit)
            throws IOException, InterruptedException {
        final Path bin = Files.createDirectories(directory.resolve("bin"));
        final Path jdk = Files.createDirectories(directory.resolve("jdk").resolve("bin"));
        final Path runnerTemp = Files.createDirectories(directory.resolve("runner"));
        Files.writeString(runnerTemp.resolve("tools.classpath"), CLASSPATH);
        final Path javaArguments = directory.resolve("java-arguments");
        Files.writeString(javaArguments, "");
        stub(
                jdk.resolve("java"),
                "printf '%s\\n' \"$@\" >> \"$JAVA_ARGUMENTS\"\nexit " + checkExit + "\n");
        // Answers only the calls the steps make, as git would; any other call fails.
        stub(
                bin.resolve("git"),
                "case \"$*\" in\n"
                        + "'cat-file -e '*) [ \"$#\" -eq 3 ] && [ \"$3\" = \"$KNOWN^{commit}\" ] ;;\n"
                        + "'rev-list -1 --before=26 hours ago HEAD')"
                        + " [ -z \"$DAY_START\" ] || printf '%s\\n' \"$DAY_START\" ;;\n"
                        + "*) echo \"unexpected git $*\" >&2; exit 99 ;;\n"
                        + "esac\n");
        final Path output = directory.resolve("output");
        final ProcessBuilder command =
                new ProcessBuilder(
                                "bash",
                                "--noprofile",
                                "--norc",
                                "-e",
                                "-o",
                                "pipefail",
                                "-c",
                                step.run())
                        .directory(directory.toFile())
                        .redirectErrorStream(true)
                        .redirectOutput(output.toFile());
        command.environment().clear();
        command.environment().put("PATH", bin + ":/usr/bin:/bin");
        command.environment().put("KNOWN", "none");
        command.environment().put("DAY_START", "");
        command.environment().putAll(environment);
        command.environment().put("JAVA_HOME_25_X64", jdk.getParent().toString());
        command.environment().put("RUNNER_TEMP", runnerTemp.toString());
        command.environment().put("JAVA_ARGUMENTS", javaArguments.toString());
        final Process process = command.start();
        try {
            assertThat(process.waitFor(10, TimeUnit.SECONDS))
                    .as("the step ends in %s", environment)
                    .isTrue();
            return new Run(
                    process.exitValue(),
                    Files.readString(output),
                    Files.readAllLines(javaArguments));
        } finally {
            process.destroyForcibly();
        }
    }

    private static void stub(final Path file, final String body) throws IOException {
        Files.writeString(file, "#!/bin/sh\n" + body);
        Files.setPosixFilePermissions(file, PosixFilePermissions.fromString("rwx------"));
    }

    /**
     * One step as written: its name ({@value #CHECKOUT} for the checkout), its own keys, its {@code
     * with:} and {@code env:} entries, and its script.
     */
    private record Step(
            String name,
            Map<String, String> keys,
            Map<String, String> with,
            Map<String, String> env,
            String run) {}

    private static Step step(final Path workflow, final String name) throws IOException {
        for (final Step step : steps(workflow)) {
            if (step.name().equals(name)) {
                return step;
            }
        }
        throw new AssertionError("no step '" + name + "' in " + workflow.getFileName());
    }

    /**
     * The steps of the workflow's first job, in order. Every line under {@code steps:} is one this
     * reading knows, or it fails: a line it skipped could hide a script or a binding from the
     * assertions.
     */
    private static List<Step> steps(final Path workflow) throws IOException {
        final List<Step> steps = new ArrayList<>();
        StepLines step = null;
        boolean inSteps = false;
        int number = 0;
        for (final String line : Files.readAllLines(workflow)) {
            number++;
            final String where = workflow.getFileName() + ":" + number;
            if (!inSteps) {
                inSteps = line.equals("        steps:");
                continue;
            }
            final int indent = line.length() - line.stripLeading().length();
            if (step != null && step.inScript() && (line.isBlank() || indent >= 18)) {
                step.script(line.isBlank() ? "" : line.substring(18));
                continue;
            }
            if (step != null) {
                step.endScript();
            }
            if (line.isBlank() || line.stripLeading().startsWith("#")) {
                continue;
            }
            if (indent < 12) {
                break;
            }
            if (indent == 12 && line.startsWith("            - ")) {
                if (step != null) {
                    steps.add(step.finish());
                }
                step = new StepLines();
                step.key(line.substring(14), where);
            } else if (step != null && indent == 14) {
                step.key(line.substring(14), where);
            } else if (step != null && indent == 18 && step.inBlock()) {
                step.entry(line.substring(18), where);
            } else {
                throw new AssertionError(where + ": a line this reading does not know: " + line);
            }
        }
        if (step != null) {
            steps.add(step.finish());
        }
        // A wrong root or a changed layout finds no step and would pass: the checkout must be read.
        assertThat(steps.stream().map(Step::name))
                .as("steps read from %s", workflow.getFileName())
                .contains(CHECKOUT);
        return steps;
    }

    /** One step while its lines are read. */
    private static final class StepLines {

        private final Map<String, String> keys = new LinkedHashMap<>();
        private final Map<String, String> with = new LinkedHashMap<>();
        private final Map<String, String> env = new LinkedHashMap<>();
        private Map<String, String> block;
        private StringBuilder script;
        private String run = "";

        void key(final String entry, final String where) {
            block = null;
            switch (entry) {
                case "with:" -> block = with;
                case "env:" -> block = env;
                case "run: |" -> {
                    put(keys, entry, where);
                    script = new StringBuilder();
                }
                default -> {
                    if (entry.startsWith("run: |") || entry.startsWith("run: >")) {
                        throw new AssertionError(
                                where + ": a script written another way: " + entry);
                    }
                    put(keys, entry, where);
                    if (entry.startsWith("run: ")) {
                        run = keys.get("run");
                    }
                }
            }
        }

        void entry(final String entry, final String where) {
            put(block, entry, where);
        }

        boolean inBlock() {
            return block != null;
        }

        boolean inScript() {
            return script != null;
        }

        void script(final String line) {
            script.append(line).append('\n');
        }

        void endScript() {
            if (script != null) {
                run = script.toString();
                script = null;
            }
        }

        Step finish() {
            endScript();
            final String uses = keys.getOrDefault("uses", "");
            final String name =
                    uses.startsWith("actions/checkout@") ? CHECKOUT : keys.getOrDefault("name", "");
            return new Step(name, keys, with, env, run);
        }
    }

    private static void put(final Map<String, String> map, final String entry, final String where) {
        final int colon = entry.indexOf(": ");
        if (colon <= 0) {
            throw new AssertionError(where + ": not a key and its value: " + entry);
        }
        final String key = entry.substring(0, colon);
        if (map.put(key, entry.substring(colon + 2).trim()) != null) {
            throw new AssertionError(where + ": '" + key + "' twice");
        }
    }
}
