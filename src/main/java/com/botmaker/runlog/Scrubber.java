package com.botmaker.runlog;

import com.botmaker.runlog.api.LogLine;

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Makes a run's text fit to leave the machine (spec {@code 2026-11-03-run-reports-design.md} §3): text read off the
 * screen ({@link ScreenText}) and each {@code hidden} text — the titles of windows other than the game's, which the
 * caller lists — become {@code …}, the home directory {@code ~}, and the user's name {@code <user>} wherever it
 * stands as a word. The local log keeps all of it; this is applied to what an issue, a comment or an export says.
 */
public final class Scrubber {

    /** What the user's name reads as. */
    public static final String USER = "<user>";

    private final List<String> hidden;
    private final Pattern home;
    private final Pattern user;

    private Scrubber(String home, String user, Collection<String> hidden) {
        this.hidden = hidden == null ? List.of() : hidden.stream()
                .filter(t -> t != null && !t.isBlank()).map(String::strip)
                // as JSON escapes it too: an export scrubs run.jsonl's raw lines
                .flatMap(t -> Stream.of(t, t.replace("\\", "\\\\").replace("\"", "\\\""))).distinct()
                .sorted(Comparator.comparingInt(String::length).reversed()).toList();
        String strip = home == null ? "" : home.strip().replaceAll("[/\\\\]+$", "");
        // Any case (Windows and macOS paths ignore it) and any separator: '/', '\', or '\\' as JSON escapes it.
        this.home = strip.length() < 2 ? null : Pattern.compile(Arrays.stream(strip.split("[/\\\\]+"))
                .map(Pattern::quote).collect(Collectors.joining("[/\\\\]+")) + "(?![\\p{L}\\p{N}_])",
                Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
        this.user = user == null || user.isBlank() ? null
                : Pattern.compile("(?<![\\p{L}\\p{N}_])" + Pattern.quote(user.strip()) + "(?![\\p{L}\\p{N}_])",
                Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    }

    /** A scrubber for {@code home} and {@code user}, hiding {@code hidden} texts too. */
    public static Scrubber of(String home, String user, Collection<String> hidden) {
        return new Scrubber(home, user, hidden);
    }

    /** A scrubber for this machine's home directory and user, hiding {@code hidden} texts too. */
    public static Scrubber forThisMachine(Collection<String> hidden) {
        return new Scrubber(System.getProperty("user.home"), System.getProperty("user.name"), hidden);
    }

    /** {@code text} scrubbed; null stays null. */
    public String scrub(String text) {
        if (text == null) return null;
        String out = ScreenText.hide(text);
        for (String h : hidden) out = out.replace(h, ScreenText.HIDDEN);
        if (home != null) out = home.matcher(out).replaceAll("~");
        return user == null ? out : user.matcher(out).replaceAll(Matcher.quoteReplacement(USER));
    }

    /** {@code line} with its text and its plugin values scrubbed. */
    public LogLine scrub(LogLine line) {
        return new LogLine(line.atMillis(), line.level(), line.source(), scrub(line.text()), line.count(), line.rect(),
                line.writerClass(), line.writerMethod(), line.className(), line.line(), scrub(line.values()));
    }

    /** {@code facts} with every text scrubbed. */
    public RunFacts scrub(RunFacts facts) {
        return new RunFacts(scrub(facts.bot()), scrub(facts.tag()), scrub(facts.commit()), facts.dirty(),
                scrub(facts.versions()), scrub(facts.os()), facts.startMillis(), scrub(facts.details()));
    }

    /** {@code map} with every key and value scrubbed. */
    public Map<String, String> scrub(Map<String, String> map) {
        Map<String, String> out = new LinkedHashMap<>();
        map.forEach((k, v) -> out.put(scrub(k), scrub(v)));
        return out;
    }
}
