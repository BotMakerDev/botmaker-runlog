package com.botmaker.runlog;

import com.botmaker.runlog.api.LogLine;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * A run's problems, as its report sends them (spec {@code 2026-11-03-run-reports-design.md} §1, §3): each warning
 * and error grouped by fingerprint, so one met 500 times is one problem ×500, in the order first met, at most
 * {@value #MAX}; the rest are counted in the last one's {@link Problem#unreported}.
 */
public final class Problems {

    /** The most problems one run reports. */
    public static final int MAX = 10;
    /** The log lines kept on either side of a problem's first line. */
    public static final int AROUND = 20;

    private Problems() {
    }

    /** The problems of the log at {@code runLog}; none when it cannot be read. */
    public static List<Problem> of(Path runLog) {
        return RunLogs.read(runLog) instanceof RunLogs.Readable r ? of(r.lines()) : List.of();
    }

    /** The problems among {@code lines}, in order. */
    public static List<Problem> of(List<LogLine> lines) {
        Map<String, List<Integer>> seen = new LinkedHashMap<>();
        for (int i = 0; i < lines.size(); i++) {
            LogLine l = lines.get(i);
            if (l.level().problem() && !host(l)) seen.computeIfAbsent(fingerprint(l), k -> new ArrayList<>()).add(i);
        }
        List<Problem> problems = new ArrayList<>();
        int unreported = Math.max(0, seen.size() - MAX);
        for (Map.Entry<String, List<Integer>> e : seen.entrySet()) {
            if (problems.size() == MAX) break;
            List<Integer> at = e.getValue();
            LogLine first = lines.get(at.getFirst());
            int count = at.stream().mapToInt(i -> lines.get(i).count()).sum();
            int from = Math.max(0, at.getFirst() - AROUND);
            int to = Math.min(lines.size(), at.getFirst() + AROUND + 1);
            problems.add(new Problem(e.getKey(), first.level(), known(first.className()), first.line(),
                    known(first.writerClass()), known(first.writerMethod()), argument(lines, at.getFirst()),
                    first.text(), count, first.atMillis(), lines.get(at.getLast()).atMillis(),
                    lines.subList(from, to), problems.size() == MAX - 1 ? unreported : 0));
        }
        return problems;
    }

    /**
     * A problem line's identity: bot class and line, called class and method, level; never its text. A line no bot
     * code led to (a plugin's own run, as Explore map) adds its plugin values, which name what it is about: one call
     * site there says every finding, and each is its own problem ({@code from=Home, to=Shop}).
     */
    static String fingerprint(LogLine l) {
        String site = botLine(l) + "|" + method(l) + "|" + l.level().id();
        if (!l.className().isEmpty() || l.values().isEmpty()) return site;
        return site + "|" + new TreeMap<>(l.values());
    }

    /** The host platform's own packages: what they say is about BotMaker on this computer, not about the bot. */
    private static final List<String> HOST = List.of("com.botmaker.shared.", "com.botmaker.session.");

    /**
     * Whether {@code l} was said by the host platform (a display that would not open, an X error trapped): never a
     * problem of the bot's, so never offered as its issue (2026-11-03, the user's call: five of seven issues of a run
     * were the display's).
     */
    static boolean host(LogLine l) {
        return HOST.stream().anyMatch(l.writerClass()::startsWith);
    }

    /** {@code class:line}, the class alone when no line, or unknown. */
    static String botLine(LogLine l) {
        return l.className().isEmpty() ? RunFacts.UNKNOWN
                : l.line() < 0 ? l.className() : l.className() + ":" + l.line();
    }

    /** {@code class.method}, the class alone when no method, or unknown. */
    static String method(LogLine l) {
        return l.writerClass().isEmpty() ? RunFacts.UNKNOWN
                : l.writerMethod().isEmpty() ? l.writerClass() : l.writerClass() + "." + l.writerMethod();
    }

    /**
     * What the bot passed to the call that led to line {@code at}: the arguments of the last call trace line before
     * it from the same bot line into the same method ({@code goTo(Home) → false} gives {@code Home}).
     */
    static String argument(List<LogLine> lines, int at) {
        LogLine problem = lines.get(at);
        if (problem.writerMethod().isEmpty()) return RunFacts.UNKNOWN;
        for (int i = at - 1; i >= 0; i--) {
            LogLine l = lines.get(i);
            if (l.level().problem() || !l.className().equals(problem.className()) || l.line() != problem.line()
                    || !l.writerClass().equals(problem.writerClass())
                    || !l.writerMethod().equals(problem.writerMethod())
                    || !l.text().startsWith(problem.writerMethod() + "(")) continue;
            String args = inParentheses(l.text(), problem.writerMethod().length());
            return args == null || args.isEmpty() ? RunFacts.UNKNOWN : args;
        }
        return RunFacts.UNKNOWN;
    }

    /** The text between the parenthesis at {@code open} and the one closing it, quotes respected; null if none. */
    private static String inParentheses(String text, int open) {
        int depth = 0;
        boolean quoted = false;
        for (int i = open; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '"' && (i == 0 || text.charAt(i - 1) != '\\')) quoted = !quoted;
            if (quoted) continue;
            if (c == '(' || c == '[' || c == '{') depth++;
            else if ((c == ')' || c == ']' || c == '}') && --depth == 0) return text.substring(open + 1, i);
        }
        return null;
    }

    private static String known(String text) {
        return text.isEmpty() ? RunFacts.UNKNOWN : text;
    }
}
