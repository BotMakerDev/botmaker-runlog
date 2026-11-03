package com.botmaker.runlog;

import com.botmaker.runlog.api.Level;
import com.botmaker.runlog.api.LogLine;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** What leaves the machine: no screen text, no home, no user name, no other window's title. */
class ScrubberTest {

    private final Scrubber scrubber = Scrubber.of("/home/ben", "ben", List.of("Inbox — Mail"));

    @Test
    void screenTextInCallResultIsHidden() {
        String text = "read(GOLD_ZONE) → " + ScreenText.mark("Gold 1,234 Ben's") + "  12 ms";
        assertEquals("read(GOLD_ZONE) → …  12 ms", scrubber.scrub(text));
        assertEquals("find(\"" + "…", scrubber.scrub("find(\"" + ScreenText.OPEN + "cut short"), "unclosed: to the end");
        Problem p = new Problem("fp", Level.WARN, "com.example.Collect", 3, "com.x.Text", "read",
                ScreenText.mark("Gold 99"), "read " + ScreenText.mark("Gold 99"), 1, 0, 0,
                List.of(new LogLine(0, Level.DEBUG, "Text", "read() → " + ScreenText.mark("Gold 99"), 1, null,
                        "com.x.Text", "read", "com.example.Collect", 3)), 0);
        String body = IssueText.body(p, RunFacts.unknown(0), scrubber);
        assertFalse(body.contains("Gold 99"), body);
        assertFalse(IssueText.comment(p, RunFacts.unknown(0), scrubber).contains("Gold 99"));
    }

    @Test
    void homeUserAndOtherWindowsAreGone() {
        assertEquals("~/bots/x.png, <user> ran it, bench stays",
                scrubber.scrub("/home/ben/bots/x.png, ben ran it, bench stays"));
        assertEquals("focus moved to …", scrubber.scrub("focus moved to Inbox — Mail"));
        assertEquals("{\"path\":\"~\\\\bots\"}",
                Scrubber.of("C:\\Users\\Ben", "Ben", List.of()).scrub("{\"path\":\"C:\\\\Users\\\\Ben\\\\bots\"}"));
        RunFacts facts = scrubber.scrub(new RunFacts("b", "v1", "c", false, Map.of(), "Linux", 0,
                Map.of("Folder", "/home/ben/game")));
        assertEquals("~/game", facts.details().get("Folder"));
        assertTrue(scrubber.scrub(new LogLine(0, Level.INFO, "S", "/home/ben", 1, null, "", "", "", -1)).text()
                .equals("~"));
    }
}
