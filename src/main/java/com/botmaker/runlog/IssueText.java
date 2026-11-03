package com.botmaker.runlog;

import com.botmaker.runlog.api.LogLine;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * What a problem says as a GitHub issue, and as a comment when it comes back (spec
 * {@code 2026-11-03-run-reports-design.md} §3). Every text is scrubbed here, by the {@link Scrubber} given, before
 * it is written; {@link RunLogs#readIssue} reads both back by their machine block's {@code format}.
 *
 * <p>The issue body ends with a fenced {@value #BLOCK} block of machine fields and a hidden
 * {@code <!-- botmaker:fp=<id> -->}, {@code <id>} being {@link #id} of the fingerprint, which a search finds again.
 * A comment ends with its own fields in a hidden {@code <!-- botmaker:{…} -->}.
 */
public final class IssueText {

    /** The info string of the issue's machine block. */
    public static final String BLOCK = "botmaker-report";

    static final Pattern FP = Pattern.compile("<!-- botmaker:fp=([0-9a-f]{16}) -->");
    static final Pattern COMMENT = Pattern.compile("<!-- botmaker:(\\{.*?}) -->", Pattern.DOTALL);
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneOffset.UTC);

    private IssueText() {
    }

    /** {@code Warning: Screens.goTo at Collect:57}. */
    public static String title(Problem p) {
        return p.level().displayName() + ": " + p.calledMethod() + " at " + p.botLine();
    }

    /** A fingerprint as the 16 hex characters an issue is found by. */
    public static String id(String fingerprint) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(fingerprint.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash, 0, 8);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    /** The hidden marker an issue for {@code fingerprint} carries. */
    public static String marker(String fingerprint) {
        return "<!-- botmaker:fp=" + id(fingerprint) + " -->";
    }

    /** The issue's body: the facts, the why, the lines around it, the machine block, the hidden fingerprint. */
    public static String body(Problem p, RunFacts facts, Scrubber scrubber) {
        RunFacts f = scrubber.scrub(facts);
        StringBuilder b = new StringBuilder();
        b.append("**").append(p.level().displayName()).append("** at `").append(span(scrubber.scrub(p.botClass())))
                .append(p.line() < 0 ? "" : ":" + p.line()).append("`, calling `").append(span(p.calledMethod()))
                .append('(').append(span(scrubber.scrub(p.argument().equals(RunFacts.UNKNOWN) ? "" : p.argument())))
                .append(")`, ×").append(p.count()).append(" in one run.\n\n");
        b.append(quote(scrubber.scrub(p.why()))).append("\n\n");
        if (p.unreported() > 0) {
            b.append("The run met ").append(p.unreported()).append(" more problem")
                    .append(p.unreported() == 1 ? "" : "s").append(", not reported.\n\n");
        }
        b.append("| Fact | Value |\n|---|---|\n");
        row(b, "Bot", f.bot());
        row(b, "Version", f.tag() + " (" + f.commit() + (f.dirty() ? ", changed" : "") + ")");
        row(b, "OS", f.os());
        f.details().forEach((k, v) -> row(b, k, v));
        f.versions().forEach((k, v) -> row(b, k, v));
        b.append('\n');
        StringBuilder log = new StringBuilder();
        for (LogLine l : p.lines()) {
            LogLine s = scrubber.scrub(l);
            log.append(TIME.format(Instant.ofEpochMilli(s.atMillis()))).append(' ').append(s.level().id())
                    .append(" [").append(s.source()).append("] ").append(lf(s.text()))
                    .append(s.count() > 1 ? "  (×" + s.count() + ")" : "").append('\n');
        }
        b.append("<details><summary>The log around it</summary>\n\n").append(fenced("text", log.toString()))
                .append("\n</details>\n\n");
        b.append(fenced(BLOCK, machine(p, f, scrubber).toPrettyString() + "\n")).append('\n');
        b.append(marker(p.fingerprint())).append('\n');
        return b.toString();
    }

    /** A comment saying {@code p} came back: {@code ×3 on v1.3, Waydroid, 2026-11-03}, its fields hidden after. */
    public static String comment(Problem p, RunFacts facts, Scrubber scrubber) {
        RunFacts f = scrubber.scrub(facts);
        StringBuilder b = new StringBuilder("×").append(p.count()).append(" on ").append(f.tag());
        f.details().values().forEach(v -> b.append(", ").append(v));
        b.append(", ").append(LocalDate.ofInstant(Instant.ofEpochMilli(p.lastMillis()), ZoneOffset.UTC)).append("\n\n");
        ObjectNode m = Format1.MAPPER.createObjectNode();
        m.put("format", RunLogs.FORMAT);
        m.put("writer", RunLogs.WRITER);
        m.put("count", p.count());
        m.put("botVersion", f.tag());
        m.put("argument", scrubber.scrub(p.argument()));
        details(m, f.details());
        b.append("<!-- botmaker:").append(hidden(m.toString())).append(" -->\n");
        return b.toString();
    }

    private static ObjectNode machine(Problem p, RunFacts f, Scrubber scrubber) {
        ObjectNode m = Format1.MAPPER.createObjectNode();
        m.put("format", RunLogs.FORMAT);
        m.put("writer", RunLogs.WRITER);
        m.put("fingerprint", scrubber.scrub(p.fingerprint()));
        m.put("level", p.level().id());
        m.put("botClass", scrubber.scrub(p.botClass()));
        m.put("line", p.line());
        m.put("calledClass", p.calledClass());
        m.put("method", p.method());
        m.put("argument", scrubber.scrub(p.argument()));
        m.put("botVersion", f.tag());
        m.put("commit", f.commit());
        m.put("count", p.count());
        details(m, f.details());
        return m;
    }

    private static void details(ObjectNode m, Map<String, String> details) {
        ObjectNode d = m.putObject("details");
        details.forEach(d::put);
    }

    /** JSON fit for an HTML comment: no {@code >} to end it early. */
    private static String hidden(String json) {
        return json.replace(">", "\\u003e");
    }

    private static void row(StringBuilder b, String fact, String value) {
        b.append("| ").append(cell(fact)).append(" | ").append(cell(value)).append(" |\n");
    }

    private static String cell(String text) {
        return lf(text).replace("|", "\\|").replace("\n", " ");
    }

    private static String quote(String text) {
        return "> " + lf(text).strip().replace("\n", "\n> ");
    }

    /** Text inside a one-backtick code span: no backtick or line break to end it early. */
    private static String span(String text) {
        return lf(text).replace('`', '\'').replace('\n', ' ');
    }

    /**
     * Every line break as {@code \n}: a lone {@code \r} breaks a Markdown line too, and would end a quote or start a
     * line {@link #block} reads, from text a runner can write.
     */
    private static String lf(String text) {
        return text.replaceAll("\r\n?", "\n");
    }

    /** {@code text} in a backtick fence longer than any run of backticks inside it. */
    static String fenced(String info, String text) {
        int longest = 0;
        Matcher m = Pattern.compile("`+").matcher(text);
        while (m.find()) longest = Math.max(longest, m.group().length());
        String fence = "`".repeat(Math.max(3, longest + 1));
        return fence + info + "\n" + text + (text.endsWith("\n") ? "" : "\n") + fence + "\n";
    }

    /**
     * The machine block's text in {@code body}, or null: the last fenced {@value #BLOCK} block, since the body writes
     * it after the log, whose lines a runner can make look like one.
     */
    static String block(String body) {
        if (body == null) return null;
        Matcher open = Pattern.compile("(?m)^(`{3,})" + BLOCK + "[ \\t]*$").matcher(body);
        int end = -1;
        String fence = null;
        while (open.find()) {
            end = open.end();
            fence = open.group(1);
        }
        if (fence == null) return null;
        int from = end + 1;
        Matcher close = Pattern.compile("(?m)^" + fence + "[ \\t]*$").matcher(body);
        return from <= body.length() && close.find(from) ? body.substring(from, close.start()) : null;
    }
}
