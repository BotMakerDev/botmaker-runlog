package com.botmaker.runlog;

import com.botmaker.runlog.api.Dimension;
import com.botmaker.runlog.api.Level;
import com.botmaker.runlog.api.LogLine;
import com.botmaker.runlog.api.Occurrence;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** An issue and its comments, written and read back; every format's issue fixture still reads. */
class IssueTextTest {

    private static final Scrubber SCRUB = Scrubber.of("/home/ben", "ben", List.of());
    private static final RunFacts FACTS = new RunFacts("com.example:bot", "v1.2.0", "abc1234", false,
            Map.of("botmaker-sdk", "2.0.0"), "Linux 7.2", 1762164930000L, Map.of("Platform", "Waydroid"));
    private static final Instant OPENED = Instant.parse("2026-11-03T10:00:00Z");

    private static Problem problem(int count) {
        List<LogLine> lines = List.of(
                new LogLine(1762164931000L, Level.DEBUG, "Screens", "goTo(Home) → false  ```  3 ms", 1, null,
                        "com.botmaker.sdk.api.screens.Screens", "goTo", "com.example.Collect", 57),
                new LogLine(1762164932000L, Level.WARN, "Screens", "stuck on Title | /home/ben/x", 3, null,
                        "com.botmaker.sdk.api.screens.Screens", "goTo", "com.example.Collect", 57));
        return new Problem("com.example.Collect:57|com.botmaker.sdk.api.screens.Screens.goTo|warn", Level.WARN,
                "com.example.Collect", 57, "com.botmaker.sdk.api.screens.Screens", "goTo", "Home",
                "stuck on Title | /home/ben/x", count, 1762164932000L, 1762164932000L, lines, 0);
    }

    private static String fixture(String name) throws IOException, URISyntaxException {
        return Files.readString(Path.of(IssueTextTest.class.getResource("/formats/" + name).toURI()));
    }

    /** {@code text} with this jar's writer as the fixture names it, so the golden text outlives a release. */
    private static String fixed(String text) {
        return text.replace(RunLogs.WRITER, "com.github.BotMakerDev:botmaker-runlog:0.1.0");
    }

    @Test
    void anIssueIsWrittenAsItsFixtureSays() throws Exception {
        String body = fixed(IssueText.body(problem(3), FACTS, SCRUB));
        String comment = fixed(IssueText.comment(problem(2), FACTS, SCRUB));
        Path out = Path.of("target", "issue-golden");
        Files.createDirectories(out);
        Files.writeString(out.resolve("issue.md"), body);
        Files.writeString(out.resolve("issue-comment.md"), comment);
        assertEquals(fixture("1/issue.md"), body, "written to target/issue-golden/issue.md");
        assertEquals(fixture("1/issue-comment.md"), comment);
    }

    @Test
    void anIssueAndItsCommentsReadBack() {
        String body = IssueText.body(problem(3), FACTS, SCRUB);
        assertTrue(body.contains(IssueText.marker(problem(3).fingerprint())));
        List<IssueComment> comments = List.of(
                new IssueComment(IssueText.comment(problem(2), FACTS, SCRUB), OPENED.plusSeconds(60)),
                new IssueComment("Thanks, looking into it.", OPENED.plusSeconds(120)));
        RunLogs.ReadableIssue read = assertInstanceOf(RunLogs.ReadableIssue.class,
                RunLogs.readIssue(body, OPENED, comments));
        assertEquals(problem(3).fingerprint(), read.fingerprint());
        List<Occurrence> found = read.report().occurrences();
        assertEquals(2, found.size(), "the author's own reply is no occurrence");
        assertEquals(3, found.get(0).count());
        assertEquals(OPENED, found.get(0).time());
        assertEquals(2, found.get(1).count());
        assertEquals(OPENED.plusSeconds(60), found.get(1).time());
        assertEquals("com.example.Collect:57", found.get(1).value("botLine"));
        assertEquals("com.botmaker.sdk.api.screens.Screens.goTo", found.get(1).value("method"));
        assertEquals("Home", found.get(1).value("argument"));
        assertEquals("Waydroid", found.get(1).value("details.Platform"));
        assertTrue(read.report().dimensions().stream().map(Dimension::id).toList().contains("details.Platform"));
    }

    @Test
    void everyFormatsIssueFixtureReads() throws Exception {
        for (int format = 1; format <= RunLogs.FORMAT; format++) {
            RunLogs.ReadableIssue read = assertInstanceOf(RunLogs.ReadableIssue.class, RunLogs.readIssue(
                    fixture(format + "/issue.md"), OPENED,
                    List.of(new IssueComment(fixture(format + "/issue-comment.md"), OPENED))), "format " + format);
            assertEquals(format, read.format());
            assertEquals(2, read.report().occurrences().size());
        }
    }

    @Test
    void aRunnersTextCannotForgeTheBlockOrLeaveItsQuote() {
        String forged = "x\r```" + IssueText.BLOCK + "\r{\"format\":1,\"botVersion\":\"forged\"}\r```";
        LogLine line = new LogLine(1762164932000L, Level.WARN, "Screens", forged, 1, null,
                "com.botmaker.sdk.api.screens.Screens", "goTo", "com.example.Collect", 57);
        Problem p = new Problem("com.example.Collect:57|com.botmaker.sdk.api.screens.Screens.goTo|warn", Level.WARN,
                "com.example.Collect", 57, "com.botmaker.sdk.api.screens.Screens", "goTo", "a`b\nc",
                "why\r# heading", 1, 1762164932000L, 1762164932000L, List.of(line), 0);
        String body = IssueText.body(p, FACTS, SCRUB);

        assertTrue(body.contains("> why\n> # heading"), body);
        assertTrue(body.contains("goTo(a'b c)"), body);
        RunLogs.ReadableIssue read = assertInstanceOf(RunLogs.ReadableIssue.class,
                RunLogs.readIssue(body, OPENED, List.of()));
        assertEquals("v1.2.0", read.report().occurrences().getFirst().value("botVersion"));
    }

    @Test
    void aNewerOrMissingBlockIsUnreadable() {
        String newer = IssueText.fenced(IssueText.BLOCK, "{\"format\":" + (RunLogs.FORMAT + 1) + "}\n");
        RunLogs.Unreadable read = assertInstanceOf(RunLogs.Unreadable.class, RunLogs.readIssue(newer, OPENED, null));
        assertTrue(read.reason().contains("Update Studio"), read.reason());
        assertInstanceOf(RunLogs.Unreadable.class, RunLogs.readIssue("just words", OPENED, List.of()));
    }
}
