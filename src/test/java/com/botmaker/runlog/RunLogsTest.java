package com.botmaker.runlog;

import com.botmaker.runlog.api.Dimension;
import com.botmaker.runlog.api.Level;
import com.botmaker.runlog.api.Occurrence;
import com.botmaker.runlog.api.Report;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Every format ever written reads through its own reader, kept in this jar; a newer one asks for an update. */
class RunLogsTest {

    @TempDir
    Path dir;

    private static Path fixture(int format) throws URISyntaxException {
        return Path.of(RunLogsTest.class.getResource("/formats/" + format + "/run.jsonl").toURI());
    }

    @Test
    void everyFormatsFixtureReads() throws URISyntaxException {
        for (int format = 1; format <= RunLogs.FORMAT; format++) {
            RunLogs.Readable read = assertInstanceOf(RunLogs.Readable.class, RunLogs.read(fixture(format)),
                    "format " + format);
            assertEquals(format, read.format());
            assertEquals(3, read.lines().size());
        }
    }

    @Test
    void format1sProblemsAreItsWarningsAndErrorsWithTheirDimensions() throws URISyntaxException {
        Report report = assertInstanceOf(RunLogs.Readable.class, RunLogs.read(fixture(1))).report();
        assertEquals(List.of("botLine", "method", "level", "botVersion", "details.Flow"),
                report.dimensions().stream().map(Dimension::id).toList());
        assertEquals("Flow", report.dimensions().getLast().label());
        List<Occurrence> found = report.occurrences();
        assertEquals(2, found.size());
        Occurrence stuck = found.getFirst();
        assertEquals(Level.WARN, stuck.level());
        assertEquals(3, stuck.count());
        assertEquals("com.example.Collect:57", stuck.value("botLine"));
        assertEquals("com.botmaker.sdk.api.screens.Screens.goTo", stuck.value("method"));
        assertEquals("v1.2.0", stuck.value("botVersion"));
        assertEquals("Collect", stuck.value("details.Flow"));
        assertEquals("com.example.Collect:57|com.botmaker.sdk.api.screens.Screens.goTo|warn", stuck.fingerprint());
    }

    @Test
    void aLogWithoutAFormatIsFormat1() throws IOException {
        Path file = dir.resolve(RunLog.FILE);
        Files.writeString(file, """
                {"facts":{"bot":"b"}}
                {"t":1,"level":"error","text":"x"}
                """);
        RunLogs.Readable read = assertInstanceOf(RunLogs.Readable.class, RunLogs.read(file));
        assertEquals(1, read.format());
        assertEquals(1, read.report().occurrences().size());
    }

    @Test
    void aNewerFormatAsksForAnUpdate() throws IOException {
        Path file = dir.resolve(RunLog.FILE);
        Files.writeString(file, "{\"format\":" + (RunLogs.FORMAT + 1) + ",\"facts\":{}}\n");
        RunLogs.Unreadable read = assertInstanceOf(RunLogs.Unreadable.class, RunLogs.read(file));
        assertTrue(read.reason().contains("Update Studio to read this log"), read.reason());
    }

    @Test
    void aFileThatIsNoLogSaysSo() throws IOException {
        Path file = dir.resolve("notes.txt");
        Files.writeString(file, "hello\n");
        assertInstanceOf(RunLogs.Unreadable.class, RunLogs.read(file));
    }
}
