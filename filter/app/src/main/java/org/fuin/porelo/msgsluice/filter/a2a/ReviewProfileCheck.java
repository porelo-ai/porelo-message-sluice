package org.fuin.porelo.msgsluice.filter.a2a;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.MissingNode;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;
import org.fuin.porelo.msgsluice.filter.decision.Finding;
import org.fuin.porelo.msgsluice.filter.decision.Severity;
import org.fuin.porelo.msgsluice.filter.detect.TextUnderCheck;
import org.jspecify.annotations.Nullable;

/**
 * A comment's data part: one review profile 2 document, walked as a closed tree to Porelo's written
 * profile and its schema. No data binding and no SARIF library: a field the profile does not list
 * is refused, never ignored, and nothing is reached by reflection. Every rule is {@code BLOCKING}
 * and an envelope rule, so it refuses also when the host runs the check reporting only.
 *
 * <p>The direction the host names decides the document's kind: what a task sends must be a
 * submission, which names no producer; what the host names inbound must be a page, whose every
 * result names its producer as the profile requires.
 */
public final class ReviewProfileCheck {

    static final String BUDGET = "PROFILE/BUDGET_EXCEEDED";

    static final String UNKNOWN_FIELD = "PROFILE/UNKNOWN_FIELD";

    static final String MISSING_FIELD = "PROFILE/MISSING_FIELD";

    static final String MALFORMED = "PROFILE/VALUE_MALFORMED";

    static final String CHARACTER = "PROFILE/FORBIDDEN_CHARACTER";

    static final String DIRECTION = "PROFILE/WRONG_DIRECTION";

    static final String PRODUCER = "PROFILE/PRODUCER_NOT_ALLOWED";

    static final String REGION = "PROFILE/REGION_TOO_LARGE";

    static final String REPLACEMENT = "PROFILE/REPLACEMENT_OUTSIDE_ANCHOR";

    static final String TEXT = "PROFILE/TEXT_TOO_LONG";

    /** Every rule this check reports. */
    public static final Set<String> RULE_IDS =
            Set.of(
                    BUDGET,
                    UNKNOWN_FIELD,
                    MISSING_FIELD,
                    MALFORMED,
                    CHARACTER,
                    DIRECTION,
                    PRODUCER,
                    REGION,
                    REPLACEMENT,
                    TEXT);

    static final int MAX_DOCUMENT_BYTES = 1024 * 1024;

    static final int MAX_RESULT_BYTES = 32 * 1024;

    static final int MAX_RESULTS = 100;

    static final int MAX_NESTING = 24;

    static final int MAX_TOKENS = 100_000;

    static final int MAX_STRING = 16_384;

    static final int MAX_NAME = 128;

    static final int MAX_NUMBER = 20;

    static final int MAX_REGION_LINES = 100;

    static final int MAX_EXTRA_LINES = 10;

    static final int MAX_LINE_CODE_POINTS = 400;

    static final int MAX_REPLACEMENT_BYTES = 8 * 1024;

    static final int MAX_MESSAGE_BYTES = 8192;

    static final int MAX_MESSAGE_WRITTEN = 16_384;

    private static final String SUBMISSION = "submission";

    private static final String PAGE = "page";

    /** The profile's https URL: no user, no query, no fragment. */
    private static final Pattern HTTPS =
            Pattern.compile(
                    "https://[A-Za-z0-9.-]+(:[0-9]{1,5})?(/[A-Za-z0-9._~!$&'()*+,;=:@%/-]*)?");

    private static final Pattern COMMIT = Pattern.compile("[0-9a-f]{40}|[0-9a-f]{64}");

    /**
     * A thread's or a forge account's id: opaque to the profile, a forge's native id or a UUID in
     * practice, so held to an id's alphabet rather than handed to the detectors.
     */
    private static final Pattern OPAQUE = Pattern.compile("[A-Za-z0-9_.:=-]{1,64}");

    /** One segment of a repository-relative path: unreserved bytes, everything else encoded. */
    private static final Pattern SEGMENT = Pattern.compile("(?:[A-Za-z0-9._~-]|%[0-9A-F]{2})++");

    private static final Pattern UUID =
            Pattern.compile("[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}");

    private static final Set<String> PAGE_ONLY =
            Set.of(
                    "porelo/threadId",
                    "porelo/threadState",
                    "porelo/sourceKind",
                    "porelo/taskId",
                    "porelo/forgeAccountId",
                    "porelo/forgeAccountName");

    private final boolean inbound;

    /**
     * @param inbound whether the host named the message inbound, from a peer it does not vouch for
     */
    public ReviewProfileCheck(final boolean inbound) {
        this.inbound = inbound;
    }

    /**
     * What the check found, and the texts the detectors judge: the prose as prose; a suggested
     * replacement, a path, a URL and a free name as code. A value its grammar fixes is not among
     * them.
     *
     * @param findings every rule that fired, each {@code BLOCKING}
     * @param texts the texts for the detectors
     */
    public record Result(List<Finding> findings, List<TextUnderCheck> texts) {

        public Result {
            findings = List.copyOf(findings);
            texts = List.copyOf(texts);
        }
    }

    /**
     * @param document the data part's value
     * @param path where it sits in the message
     * @param partIndex its part's index
     */
    public Result check(final JsonNode document, final String path, final int partIndex) {
        final Walk walk = new Walk(partIndex);
        final @Nullable String over = Budget.over(document);
        if (over != null) {
            walk.refuse(BUDGET, path, "The review document exceeds a bound: " + over + ".");
            return walk.result();
        }
        walk.characters(document, path);
        walk.root(document, path);
        return walk.result();
    }

    /** One walk over one document. */
    private final class Walk {

        private final int partIndex;

        private final List<Finding> findings = new ArrayList<>();

        private final List<TextUnderCheck> texts = new ArrayList<>();

        private final Set<String> reported = new HashSet<>();

        Walk(final int partIndex) {
            this.partIndex = partIndex;
        }

        Result result() {
            return new Result(findings, texts);
        }

        void refuse(final String rule, final String location, final String reason) {
            // One finding per rule and place: a hundred results with one fault are one reason.
            if (reported.add(rule + " " + location)) {
                findings.add(
                        Finding.envelope(rule, Severity.BLOCKING, location, partIndex, reason));
            }
        }

        // The tree, as the profile's schema closes it.

        void root(final JsonNode doc, final String at) {
            final @Nullable Fields f =
                    object(doc, at, Set.of("version", "$schema", "properties", "runs"), Set.of());
            if (f == null) {
                return;
            }
            constant(f, "version", "2.1.0", at);
            constant(f, "$schema", "https://json.schemastore.org/sarif-2.1.0.json", at);
            final String pat = at + ".properties";
            final @Nullable Fields props =
                    object(
                            f.get("properties"),
                            pat,
                            Set.of("porelo/profileVersion", "porelo/documentKind"),
                            Set.of());
            if (props == null) {
                return;
            }
            constant(props, "porelo/profileVersion", "2", pat);
            oneOf(props, "porelo/documentKind", Set.of(PAGE, SUBMISSION), pat);
            final String kind = props.get("porelo/documentKind").asText("");
            final boolean submission = SUBMISSION.equals(kind);
            if (!PAGE.equals(kind) && !submission) {
                return;
            }
            if (submission == inbound) {
                refuse(
                        DIRECTION,
                        pat + ".porelo/documentKind",
                        inbound
                                ? "A message the host names inbound carries a page, never a submission."
                                : "A task sends a submission; a page passes only when the host names"
                                        + " the message inbound.");
                return;
            }
            final JsonNode runs = f.get("runs");
            if (!runs.isArray() || runs.size() != 1) {
                refuse(MALFORMED, at + ".runs", "A review document holds exactly one run.");
                return;
            }
            run(runs.get(0), at + ".runs[0]", submission);
        }

        void run(final JsonNode run, final String at, final boolean submission) {
            final @Nullable Fields f =
                    object(
                            run,
                            at,
                            Set.of(
                                    "tool",
                                    "versionControlProvenance",
                                    "columnKind",
                                    "results",
                                    "properties"),
                            Set.of());
            if (f == null) {
                return;
            }
            final @Nullable Fields tool =
                    object(f.get("tool"), at + ".tool", Set.of("driver"), Set.of());
            if (tool != null) {
                final @Nullable Fields driver =
                        object(tool.get("driver"), at + ".tool.driver", Set.of("name"), Set.of());
                if (driver != null) {
                    constant(driver, "name", "Porelo", at + ".tool.driver");
                }
            }
            constant(f, "columnKind", "unicodeCodePoints", at);
            final String pat = at + ".properties";
            // A comment's document is a pull request's, in either direction: a forge has no push
            // of Porelo's, and a task submits only to a pull request's review.
            final @Nullable Fields props =
                    object(
                            f.get("properties"),
                            pat,
                            Set.of("porelo/pullRequest"),
                            Set.of("porelo/baseRevisionId", "porelo/completeness"));
            if (props != null) {
                url(props, "porelo/pullRequest", pat);
                grammar(props, "porelo/baseRevisionId", COMMIT, pat, "a full commit id");
                if (submission && props.containsKey("porelo/completeness")) {
                    refuse(
                            MALFORMED,
                            pat + ".porelo/completeness",
                            "A submission is no page and carries no completeness.");
                } else if (!submission && !props.containsKey("porelo/completeness")) {
                    refuse(
                            MISSING_FIELD,
                            pat + ".porelo/completeness",
                            "A page says whether it is complete.");
                }
                oneOf(
                        props,
                        "porelo/completeness",
                        Set.of("complete", "partial", "failed", "unknown"),
                        pat);
            }
            provenance(f.get("versionControlProvenance"), at + ".versionControlProvenance");
            final JsonNode results = f.get("results");
            if (!results.isArray()) {
                refuse(MALFORMED, at + ".results", "The results are a list.");
                return;
            }
            if (submission && results.isEmpty()) {
                refuse(MISSING_FIELD, at + ".results", "A submission holds at least one result.");
            }
            for (int i = 0; i < results.size(); i++) {
                result(results.get(i), at + ".results[" + i + "]", submission);
            }
        }

        void provenance(final JsonNode vcp, final String at) {
            if (!vcp.isArray() || vcp.size() != 1) {
                refuse(MALFORMED, at, "A review document names exactly one repository.");
                return;
            }
            final String vat = at + "[0]";
            final @Nullable Fields v =
                    object(vcp.get(0), vat, Set.of("repositoryUri", "revisionId"), Set.of());
            if (v == null) {
                return;
            }
            grammar(v, "revisionId", COMMIT, vat, "a full commit id");
            url(v, "repositoryUri", vat);
        }

        void result(final JsonNode r, final String at, final boolean submission) {
            final @Nullable Fields f =
                    object(
                            r,
                            at,
                            Set.of("kind", "level", "message", "locations", "properties"),
                            Set.of("guid", "fixes"));
            if (f == null) {
                return;
            }
            if (submission && f.containsKey("guid")) {
                refuse(
                        PRODUCER,
                        at + ".guid",
                        "A submission carries no guid: Porelo gives each item its own.");
            } else if (!submission && !f.containsKey("guid")) {
                refuse(MISSING_FIELD, at + ".guid", "Each result of a page has its guid.");
            } else {
                grammar(f, "guid", UUID, at, "a UUID of version 4");
            }
            constant(f, "kind", "review", at);
            constant(f, "level", "none", at);
            final @Nullable Fields message =
                    object(f.get("message"), at + ".message", Set.of("text"), Set.of());
            if (message != null) {
                message(message.get("text"), at + ".message.text");
            }
            final @Nullable Region anchor = locations(f.get("locations"), at + ".locations");
            final @Nullable Fields props =
                    properties(f.get("properties"), at + ".properties", submission);
            if (f.containsKey("fixes")) {
                final boolean reply = props != null && props.containsKey("porelo/replyTo");
                final boolean newSide =
                        props != null && "new".equals(props.get("porelo/diffSide").asText(""));
                if (reply || !newSide) {
                    refuse(
                            MALFORMED,
                            at + ".fixes",
                            "Only a new comment on the new side carries a suggestion.");
                } else {
                    fixes(f.get("fixes"), at + ".fixes", anchor);
                }
            }
        }

        @Nullable Fields properties(
                final JsonNode node, final String at, final boolean submission) {
            final Set<String> optional = new HashSet<>(PAGE_ONLY);
            optional.add("porelo/replyTo");
            final @Nullable Fields f =
                    object(node, at, Set.of("porelo/anchorKind", "porelo/diffSide"), optional);
            if (f == null) {
                return null;
            }
            oneOf(f, "porelo/anchorKind", Set.of("line"), at);
            oneOf(f, "porelo/diffSide", Set.of("old", "new"), at);
            opaque(f, "porelo/replyTo", at);
            if (submission) {
                for (final String name : new TreeSet<>(PAGE_ONLY)) {
                    if (f.containsKey(name)) {
                        refuse(
                                PRODUCER,
                                at + "." + name,
                                "A submission names no producer, thread or thread state: the"
                                        + " channel it comes through stamps them.");
                    }
                }
                return f;
            }
            for (final String name :
                    List.of("porelo/threadId", "porelo/threadState", "porelo/sourceKind")) {
                if (!f.containsKey(name)) {
                    refuse(MISSING_FIELD, at + "." + name, "Each result of a page names it.");
                }
            }
            opaque(f, "porelo/threadId", at);
            oneOf(f, "porelo/threadState", Set.of("open", "resolved"), at);
            oneOf(f, "porelo/sourceKind", Set.of("owner-account", "task", "forge-account"), at);
            producer(f, at);
            return f;
        }

        /**
         * A page comes from a forge's adapter, which knows no task and no owner account: every
         * result is a forge account's, with its id and name, and names no task.
         */
        void producer(final Fields f, final String at) {
            if (f.containsKey("porelo/sourceKind")
                    && !"forge-account".equals(f.get("porelo/sourceKind").asText(""))) {
                refuse(
                        PRODUCER,
                        at + ".porelo/sourceKind",
                        "A forge's page holds only forge accounts' comments.");
            }
            if (f.containsKey("porelo/taskId")) {
                refuse(PRODUCER, at + ".porelo/taskId", "A forge's page names no task.");
            }
            for (final String name : List.of("porelo/forgeAccountId", "porelo/forgeAccountName")) {
                if (!f.containsKey(name)) {
                    refuse(
                            PRODUCER,
                            at + "." + name,
                            "Each result of a forge's page names the forge account that wrote it.");
                }
            }
            opaque(f, "porelo/forgeAccountId", at);
            name(f, "porelo/forgeAccountName", 256, at);
        }

        @Nullable Region locations(final JsonNode locations, final String at) {
            if (!locations.isArray() || locations.size() != 1) {
                refuse(MALFORMED, at, "A result has exactly one location.");
                return null;
            }
            final String lat = at + "[0]";
            final @Nullable Fields f =
                    object(locations.get(0), lat, Set.of("physicalLocation"), Set.of());
            if (f == null) {
                return null;
            }
            final String pat = lat + ".physicalLocation";
            final @Nullable Fields p =
                    object(
                            f.get("physicalLocation"),
                            pat,
                            Set.of("artifactLocation", "region"),
                            Set.of());
            if (p == null) {
                return null;
            }
            final @Nullable String uri =
                    artifact(p.get("artifactLocation"), pat + ".artifactLocation");
            final @Nullable Region region = region(p.get("region"), pat + ".region");
            return uri == null || region == null ? null : region.in(uri);
        }

        void fixes(final JsonNode fixes, final String at, final @Nullable Region anchor) {
            if (!fixes.isArray() || fixes.size() != 1) {
                refuse(MALFORMED, at, "A comment carries at most one suggestion.");
                return;
            }
            final String fat = at + "[0]";
            final @Nullable Fields f =
                    object(fixes.get(0), fat, Set.of("artifactChanges"), Set.of());
            if (f == null) {
                return;
            }
            final JsonNode changes = f.get("artifactChanges");
            if (!changes.isArray() || changes.size() != 1) {
                refuse(
                        MALFORMED,
                        fat + ".artifactChanges",
                        "A suggestion changes exactly one file.");
                return;
            }
            final String cat = fat + ".artifactChanges[0]";
            final @Nullable Fields c =
                    object(
                            changes.get(0),
                            cat,
                            Set.of("artifactLocation", "replacements"),
                            Set.of());
            if (c == null) {
                return;
            }
            final @Nullable String uri =
                    artifact(c.get("artifactLocation"), cat + ".artifactLocation");
            final JsonNode replacements = c.get("replacements");
            if (!replacements.isArray() || replacements.size() != 1) {
                refuse(
                        MALFORMED,
                        cat + ".replacements",
                        "A suggestion is exactly one replacement.");
                return;
            }
            final String rat = cat + ".replacements[0]";
            final @Nullable Fields r =
                    object(
                            replacements.get(0),
                            rat,
                            Set.of("deletedRegion", "insertedContent"),
                            Set.of());
            if (r == null) {
                return;
            }
            final @Nullable Region deleted = region(r.get("deletedRegion"), rat + ".deletedRegion");
            if (anchor != null && deleted != null && !anchor.equals(deleted.in(uri))) {
                refuse(
                        REPLACEMENT,
                        rat,
                        "A suggestion replaces exactly the lines its comment is anchored to, in"
                                + " the same file.");
            }
            final @Nullable Fields inserted =
                    object(
                            r.get("insertedContent"),
                            rat + ".insertedContent",
                            Set.of("text"),
                            Set.of());
            if (inserted != null) {
                replacement(inserted.get("text"), rat + ".insertedContent.text", deleted);
            }
        }

        @Nullable String artifact(final JsonNode node, final String at) {
            final @Nullable Fields f = object(node, at, Set.of("uri"), Set.of());
            if (f == null) {
                return null;
            }
            final JsonNode uri = f.get("uri");
            if (!uri.isTextual() || !canonicalPath(uri.asText())) {
                refuse(
                        MALFORMED,
                        at + ".uri",
                        "A file is named by a canonical repository-relative path: no scheme, no"
                                + " leading '/', no empty, '.' or '..' segment, every byte but the"
                                + " unreserved ones percent-encoded in upper case.");
                return null;
            }
            texts.add(TextUnderCheck.code(at + ".uri", partIndex, uri.asText()));
            return uri.asText();
        }

        @Nullable Region region(final JsonNode node, final String at) {
            final @Nullable Fields f = object(node, at, Set.of("startLine", "endLine"), Set.of());
            if (f == null) {
                return null;
            }
            final int start = line(f.get("startLine"), at + ".startLine");
            final int end = line(f.get("endLine"), at + ".endLine");
            if (start < 1 || end < 1) {
                return null;
            }
            if (end < start) {
                refuse(MALFORMED, at, "A region ends at or after its first line.");
                return null;
            }
            if ((long) end - start + 1 > MAX_REGION_LINES) {
                refuse(REGION, at, "A region spans at most " + MAX_REGION_LINES + " lines.");
                return null;
            }
            return new Region(null, start, end);
        }

        int line(final JsonNode node, final String at) {
            if (!node.isIntegralNumber() || !node.canConvertToInt() || node.asInt() < 1) {
                refuse(MALFORMED, at, "A line is a whole number from 1.");
                return -1;
            }
            return node.asInt();
        }

        /**
         * A comment's text: plain text with something in it, bounded by its written form and by its
         * bytes as given, before SARIF's escaping.
         */
        void message(final JsonNode node, final String at) {
            if (!node.isTextual()
                    || blank(node.asText())
                    || node.asText().length() > MAX_MESSAGE_WRITTEN) {
                refuse(
                        MALFORMED,
                        at,
                        "A comment's text is a string of 1 to "
                                + MAX_MESSAGE_WRITTEN
                                + " characters as written, not only white space.");
                return;
            }
            final String text = node.asText();
            if (utf8(unescape(text)) > MAX_MESSAGE_BYTES) {
                refuse(
                        TEXT,
                        at,
                        "A comment's text is at most "
                                + MAX_MESSAGE_BYTES
                                + " bytes of UTF-8 as given.");
            }
            texts.add(new TextUnderCheck(at, partIndex, text));
        }

        void replacement(final JsonNode node, final String at, final @Nullable Region deleted) {
            if (!node.isTextual()) {
                refuse(MALFORMED, at, "The suggested text is a string.");
                return;
            }
            final String text = node.asText();
            if (utf8(text) > MAX_REPLACEMENT_BYTES) {
                refuse(
                        TEXT,
                        at,
                        "A suggestion is at most " + MAX_REPLACEMENT_BYTES + " bytes of UTF-8.");
            }
            final String[] lines = text.split("\n", -1);
            final int count = text.endsWith("\n") ? lines.length - 1 : lines.length;
            if (deleted != null && count > deleted.end() - deleted.start() + 1 + MAX_EXTRA_LINES) {
                refuse(
                        REPLACEMENT,
                        at,
                        "A suggestion replaces its anchor's lines with at most "
                                + MAX_EXTRA_LINES
                                + " lines more.");
            }
            for (final String line : lines) {
                if (line.codePointCount(0, line.length()) > MAX_LINE_CODE_POINTS) {
                    refuse(
                            TEXT,
                            at,
                            "A suggested line is at most " + MAX_LINE_CODE_POINTS + " characters.");
                    break;
                }
            }
            texts.add(TextUnderCheck.code(at, partIndex, text));
        }

        // The pieces.

        /**
         * An object holding every required field and nothing but the required and optional ones;
         * {@code null} when it is not an object or misses a field, which is reported.
         */
        @Nullable Fields object(
                final @Nullable JsonNode node,
                final String at,
                final Set<String> required,
                final Set<String> optional) {
            if (node == null) {
                refuse(MISSING_FIELD, at, "The field is required.");
                return null;
            }
            if (!node.isObject()) {
                refuse(MALFORMED, at, "The field is an object.");
                return null;
            }
            final Map<String, JsonNode> fields = new LinkedHashMap<>();
            int i = 0;
            for (final Iterator<Map.Entry<String, JsonNode>> it = node.fields();
                    it.hasNext();
                    i++) {
                final Map.Entry<String, JsonNode> e = it.next();
                if (required.contains(e.getKey()) || optional.contains(e.getKey())) {
                    fields.put(e.getKey(), e.getValue());
                } else {
                    refuse(
                            UNKNOWN_FIELD,
                            at + ".#" + i,
                            "The field is not part of review profile 2.");
                }
            }
            boolean complete = true;
            for (final String name : new TreeSet<>(required)) {
                if (!fields.containsKey(name)) {
                    refuse(MISSING_FIELD, at + "." + name, "The field is required.");
                    complete = false;
                }
            }
            return complete ? new Fields(fields) : null;
        }

        void constant(final Fields f, final String name, final String value, final String at) {
            final JsonNode node = f.get(name);
            if (!node.isMissingNode() && !(node.isTextual() && node.asText().equals(value))) {
                refuse(MALFORMED, at + "." + name, "The field must be '" + value + "'.");
            }
        }

        void oneOf(final Fields f, final String name, final Set<String> values, final String at) {
            final JsonNode node = f.get(name);
            if (!node.isMissingNode() && !(node.isTextual() && values.contains(node.asText()))) {
                refuse(
                        MALFORMED,
                        at + "." + name,
                        "The field is one of " + new TreeSet<>(values) + ".");
            }
        }

        void grammar(
                final Fields f,
                final String name,
                final Pattern pattern,
                final String at,
                final String what) {
            final JsonNode node = f.get(name);
            if (!node.isMissingNode()
                    && !(node.isTextual() && pattern.matcher(node.asText()).matches())) {
                refuse(MALFORMED, at + "." + name, "The field must be " + what + ".");
            }
        }

        /**
         * An id by its grammar, and judged as code as well: its alphabet is base64url's, so its
         * shape says nothing about what it carries, and a forge's numeric id trips nothing.
         */
        void opaque(final Fields f, final String name, final String at) {
            grammar(
                    f,
                    name,
                    OPAQUE,
                    at,
                    "an id of 1 to 64 letters, digits, '_', '.', ':', '=' or '-'");
            code(f.get(name), at + "." + name);
        }

        /** A free name of bounded length, judged as code, since its writer chose it. */
        void name(final Fields f, final String name, final int max, final String at) {
            final JsonNode node = f.get(name);
            if (node.isMissingNode()) {
                return;
            }
            if (!node.isTextual()
                    || node.asText().isEmpty()
                    || node.asText().length() > max
                    || node.asText().indexOf('\n') >= 0
                    || node.asText().indexOf('\t') >= 0) {
                refuse(
                        MALFORMED,
                        at + "." + name,
                        "The field is one line of 1 to " + max + " characters.");
                return;
            }
            code(node, at + "." + name);
        }

        /** An https URL by its grammar, and judged as code, since its path is free text. */
        void url(final Fields f, final String name, final String at) {
            grammar(f, name, HTTPS, at, "an https URL without user, query or fragment");
            code(f.get(name), at + "." + name);
        }

        void code(final @Nullable JsonNode node, final String at) {
            if (node != null && node.isTextual()) {
                texts.add(TextUnderCheck.code(at, partIndex, node.asText()));
            }
        }

        /**
         * No character that hides or reorders text anywhere in the document, keys included: format
         * characters, variation selectors, line and paragraph separators, lone surrogates, and
         * control characters other than line feed and tab.
         */
        void characters(final JsonNode node, final String at) {
            if (node.isObject()) {
                int i = 0;
                for (final Iterator<Map.Entry<String, JsonNode>> it = node.fields();
                        it.hasNext();
                        i++) {
                    final Map.Entry<String, JsonNode> e = it.next();
                    if (forbidden(e.getKey())) {
                        refuse(
                                CHARACTER,
                                at + ".#" + i,
                                "A field name holds a hidden or control character.");
                    }
                    characters(e.getValue(), at + ".#" + i);
                }
            } else if (node.isArray()) {
                for (int i = 0; i < node.size(); i++) {
                    characters(node.get(i), at + "[" + i + "]");
                }
            } else if (node.isTextual() && forbidden(node.asText())) {
                refuse(
                        CHARACTER,
                        at,
                        "The text holds a hidden or control character: a format character, a"
                                + " variation selector, a line or paragraph separator, a lone"
                                + " surrogate or a control character other than line feed and"
                                + " tab.");
            }
        }
    }

    /** An object's fields as the walk admitted them; an absent one reads as a missing node. */
    private record Fields(Map<String, JsonNode> map) {

        JsonNode get(final String name) {
            final JsonNode node = map.get(name);
            return node == null ? MissingNode.getInstance() : node;
        }

        boolean containsKey(final String name) {
            return map.containsKey(name);
        }
    }

    /**
     * Lines of a file, inclusive, counted from 1.
     *
     * @param uri the file, or {@code null} while it is not known
     */
    private record Region(@Nullable String uri, int start, int end) {

        Region in(final @Nullable String file) {
            return new Region(file, start, end);
        }
    }

    /**
     * Whether a text holds nothing but white space, as the schema's ECMAScript {@code \\S} reads
     * it.
     */
    static boolean blank(final String text) {
        return text.codePoints()
                .allMatch(
                        cp ->
                                Character.isWhitespace(cp)
                                        || Character.isSpaceChar(cp)
                                        || cp == 0xFEFF);
    }

    /** Whether a text holds a character that hides or reorders what a reader sees. */
    static boolean forbidden(final String text) {
        for (int i = 0; i < text.length(); ) {
            final char c = text.charAt(i);
            if (Character.isLowSurrogate(c)
                    || (Character.isHighSurrogate(c)
                            && (i + 1 >= text.length()
                                    || !Character.isLowSurrogate(text.charAt(i + 1))))) {
                return true;
            }
            final int cp = text.codePointAt(i);
            final int type = Character.getType(cp);
            if (type == Character.FORMAT
                    || type == Character.LINE_SEPARATOR
                    || type == Character.PARAGRAPH_SEPARATOR
                    || (type == Character.CONTROL && cp != '\n' && cp != '\t')
                    || variationSelector(cp)) {
                return true;
            }
            i += Character.charCount(cp);
        }
        return false;
    }

    private static boolean variationSelector(final int cp) {
        return (cp >= 0xFE00 && cp <= 0xFE0F)
                || (cp >= 0xE0100 && cp <= 0xE01EF)
                || (cp >= 0x180B && cp <= 0x180D)
                || cp == 0x180F;
    }

    /**
     * A canonical repository-relative path: segments of unreserved bytes and upper-case
     * percent-escapes, none empty, {@code .} or {@code ..}, and no escape of an unreserved byte.
     */
    static boolean canonicalPath(final String uri) {
        if (uri.isEmpty() || uri.length() > 12_288) {
            return false;
        }
        for (final String segment : uri.split("/", -1)) {
            if (segment.isEmpty()
                    || segment.equals(".")
                    || segment.equals("..")
                    || !SEGMENT.matcher(segment).matches()) {
                return false;
            }
            for (int i = segment.indexOf('%'); i >= 0; i = segment.indexOf('%', i + 1)) {
                if (unreserved(Integer.parseInt(segment.substring(i + 1, i + 3), 16))) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean unreserved(final int b) {
        return (b >= 'A' && b <= 'Z')
                || (b >= 'a' && b <= 'z')
                || (b >= '0' && b <= '9')
                || b == '-'
                || b == '.'
                || b == '_'
                || b == '~';
    }

    /**
     * A message's text as given: SARIF's escapes undone, a backslash before a backslash or a square
     * bracket, and a doubled brace.
     */
    static String unescape(final String written) {
        final StringBuilder given = new StringBuilder(written.length());
        for (int i = 0; i < written.length(); i++) {
            final char c = written.charAt(i);
            final char next = i + 1 < written.length() ? written.charAt(i + 1) : 0;
            if (c == '\\' && (next == '\\' || next == '[' || next == ']')
                    || (c == '{' || c == '}') && next == c) {
                given.append(next);
                i++;
            } else {
                given.append(c);
            }
        }
        return given.toString();
    }

    private static int utf8(final String text) {
        return text.getBytes(StandardCharsets.UTF_8).length;
    }

    /** The parser budgets, held over the tree before the walk reads a field. */
    static final class Budget {

        private Budget() {}

        /** What bound the document exceeds, said without quoting it, or {@code null}. */
        static @Nullable String over(final JsonNode document) {
            final long[] tokens = {0};
            final @Nullable String found = walk(document, 1, tokens);
            if (found != null) {
                return found;
            }
            if (bytes(document) > MAX_DOCUMENT_BYTES) {
                return "more than " + MAX_DOCUMENT_BYTES + " bytes";
            }
            final JsonNode results = document.path("runs").path(0).path("results");
            if (results.isArray()) {
                if (results.size() > MAX_RESULTS) {
                    return "more than " + MAX_RESULTS + " results";
                }
                for (final JsonNode result : results) {
                    if (bytes(result) > MAX_RESULT_BYTES) {
                        return "a result of more than " + MAX_RESULT_BYTES + " bytes";
                    }
                }
            }
            return null;
        }

        private static @Nullable String walk(
                final JsonNode node, final int depth, final long[] tokens) {
            if (depth > MAX_NESTING) {
                return "nesting deeper than " + MAX_NESTING;
            }
            tokens[0]++;
            if (node.isObject()) {
                for (final Iterator<Map.Entry<String, JsonNode>> it = node.fields();
                        it.hasNext(); ) {
                    final Map.Entry<String, JsonNode> e = it.next();
                    tokens[0]++;
                    if (e.getKey().length() > MAX_NAME) {
                        return "a field name longer than " + MAX_NAME + " characters";
                    }
                    final @Nullable String found = walk(e.getValue(), depth + 1, tokens);
                    if (found != null) {
                        return found;
                    }
                }
                tokens[0]++;
            } else if (node.isArray()) {
                for (final JsonNode element : node) {
                    final @Nullable String found = walk(element, depth + 1, tokens);
                    if (found != null) {
                        return found;
                    }
                }
                tokens[0]++;
            } else if (node.isTextual()) {
                if (node.asText().length() > MAX_STRING) {
                    return "a string longer than " + MAX_STRING + " characters";
                }
            } else if (node.isNumber()) {
                if (tooLong(node)) {
                    return "a number longer than " + MAX_NUMBER + " digits";
                }
            }
            return tokens[0] > MAX_TOKENS ? "more than " + MAX_TOKENS + " tokens" : null;
        }

        /**
         * Bounded by its digits and its exponent, never by its plain text: 1e999999999 is short as
         * written and a billion digits as read.
         */
        private static boolean tooLong(final JsonNode number) {
            if (number.isIntegralNumber()) {
                return number.bigIntegerValue()
                                .abs()
                                .compareTo(java.math.BigInteger.TEN.pow(MAX_NUMBER))
                        >= 0;
            }
            final java.math.BigDecimal value = number.decimalValue();
            return value.precision() > MAX_NUMBER || Math.abs((long) value.scale()) > MAX_NUMBER;
        }

        /** The size as compact UTF-8 JSON, the form the profile's bounds count. */
        private static long bytes(final JsonNode node) {
            return node.toString().getBytes(StandardCharsets.UTF_8).length;
        }
    }
}
