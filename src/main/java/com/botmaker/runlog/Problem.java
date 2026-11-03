package com.botmaker.runlog;

import com.botmaker.runlog.api.Level;
import com.botmaker.runlog.api.LogLine;

import java.util.List;

/**
 * One problem of a run (spec {@code 2026-11-03-run-reports-design.md} §1, §3): every warning or error with one
 * {@link #fingerprint}, met {@link #count} times, as {@link Problems#of} finds it.
 *
 * @param fingerprint its identity: bot class and line, called class and method, level; never the why's text
 * @param level       {@link Level#WARN} or {@link Level#ERROR}
 * @param botClass    the bot's class whose call led to it, or {@value RunFacts#UNKNOWN}
 * @param line        that class's line, or -1
 * @param calledClass the class that said it, or {@value RunFacts#UNKNOWN}
 * @param method      its method, or {@value RunFacts#UNKNOWN}
 * @param argument    what the bot's call passed, as the call's trace line shows it, or {@value RunFacts#UNKNOWN}
 * @param why         what the first one said
 * @param count       how many times it was met
 * @param firstMillis when it was first met
 * @param lastMillis  when last
 * @param lines       the log's lines around the first one, it included
 * @param unreported  how many more problems the run met past the quota, counted in the last one; 0 for any other
 */
public record Problem(String fingerprint, Level level, String botClass, int line, String calledClass, String method,
                      String argument, String why, int count, long firstMillis, long lastMillis, List<LogLine> lines,
                      int unreported) {

    public Problem {
        lines = List.copyOf(lines);
    }

    /** {@code Collect:57}, the bot class's simple name and its line. */
    public String botLine() {
        return simple(botClass) + (line < 0 ? "" : ":" + line);
    }

    /** {@code Screens.goTo}, the called class's simple name and its method. */
    public String calledMethod() {
        return simple(calledClass) + (method.equals(RunFacts.UNKNOWN) ? "" : "." + method);
    }

    static String simple(String className) {
        return className.substring(className.lastIndexOf('.') + 1);
    }
}
