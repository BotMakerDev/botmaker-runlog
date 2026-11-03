package com.botmaker.runlog;

import com.botmaker.runlog.api.Level;
import com.botmaker.runlog.api.LogLine;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** A run's problems: grouped by fingerprint, capped at ten, each with its argument and the lines around it. */
class ProblemsTest {

    private static final String SCREENS = "com.botmaker.sdk.api.screens.Screens";

    private static LogLine call(long t, String text) {
        return new LogLine(t, Level.DEBUG, "Screens", text, 1, null, SCREENS, "goTo", "com.example.Collect", 57);
    }

    private static LogLine warn(long t, String why, String method, int line) {
        return new LogLine(t, Level.WARN, "Screens", why, 1, null, SCREENS, method, "com.example.Collect", line);
    }

    @Test
    void repeatsCollapse() {
        List<LogLine> lines = new ArrayList<>();
        for (int i = 0; i < 500; i++) {
            lines.add(call(i * 10L, "goTo(Home) → false  3 ms"));
            lines.add(warn(i * 10L + 1, "stuck on Title, score " + i, "goTo", 57));
        }
        List<Problem> problems = Problems.of(lines);
        assertEquals(1, problems.size());
        Problem p = problems.getFirst();
        assertEquals(500, p.count());
        assertEquals("stuck on Title, score 0", p.why());
        assertEquals("Home", p.argument());
        assertEquals("com.example.Collect:57|" + SCREENS + ".goTo|warn", p.fingerprint());
        assertEquals(1, p.firstMillis());
        assertEquals(4991, p.lastMillis());
        assertEquals(Problems.AROUND + 2, p.lines().size(), "its call, the first one and the 20 after it");
        assertEquals("Warning: Screens.goTo at Collect:57", IssueText.title(p));
    }

    @Test
    void tenAtMostTheRestCountedInTheLast() {
        List<LogLine> lines = new ArrayList<>();
        for (int i = 0; i < 13; i++) lines.add(warn(i, "slow", "goTo", i));
        List<Problem> problems = Problems.of(lines);
        assertEquals(Problems.MAX, problems.size());
        assertEquals(3, problems.getLast().unreported());
        assertEquals(0, problems.getFirst().unreported());
    }

    @Test
    void anArgumentComesOnlyFromItsOwnCall() {
        List<LogLine> lines = List.of(
                call(1, "goTo(Shop(\"a)b\"), [1, 2]) → false"),
                warn(2, "stuck", "goTo", 57),
                warn(3, "stuck", "goTo", 58));
        List<Problem> problems = Problems.of(lines);
        assertEquals("Shop(\"a)b\"), [1, 2]", problems.get(0).argument());
        assertEquals(RunFacts.UNKNOWN, problems.get(1).argument(), "line 58 made no traced call");
    }
}
