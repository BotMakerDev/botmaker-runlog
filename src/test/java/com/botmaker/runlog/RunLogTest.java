package com.botmaker.runlog;

import com.botmaker.runlog.api.Level;
import com.botmaker.runlog.api.LogLine;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** A run's log: its facts first, one JSON object per line, five runs kept, a cap, a torn last line skipped. */
class RunLogTest {

    @TempDir
    Path root;

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-03T10:15:30Z"), ZoneId.of("UTC"));

    private static RunFacts facts() {
        return new RunFacts("com.example:bot", "v1.2.0", "abc1234", false, Map.of("botmaker-sdk", "2.0.0"),
                "Linux 7.2", 1L, Map.of("Flow", "Collect", "Game version", "1.4.2 (142)"));
    }

    private static LogLine line(Level level, String text) {
        return new LogLine(5L, level, "Vision", text, 2, new LogLine.Rect(1, 2, 3, 4),
                "com.botmaker.sdk.api.vision.ImageFinder", "find", "com.example.Collect", 42);
    }

    private static RunLogs.Readable read(Path file) {
        return assertInstanceOf(RunLogs.Readable.class, RunLogs.read(file));
    }

    @Test
    void factsAreTheFirstLineAndEachLineReadsBack() throws IOException {
        try (RunLog log = RunLog.start(root, facts(), CLOCK)) {
            log.write(line(Level.DEBUG, "find(ORE) → miss"));
            log.write(line(Level.WARN, "stuck on Home"));
            assertEquals("20261103-101530", log.folder().getFileName().toString());
        }
        Path file = root.resolve("20261103-101530").resolve(RunLog.FILE);
        List<String> raw = Files.readAllLines(file);
        assertEquals(3, raw.size());
        assertTrue(raw.getFirst().startsWith("{\"format\":1,\"writer\":\"com.github.BotMakerDev:botmaker-runlog:"),
                raw.getFirst());
        RunLogs.Readable read = read(file);
        assertEquals(1, read.format());
        assertEquals(facts(), read.facts());
        assertEquals(List.of(line(Level.DEBUG, "find(ORE) → miss"), line(Level.WARN, "stuck on Home")), read.lines());
    }

    @Test
    void aPluginsValuesReadBackAndBecomeDimensions() throws IOException {
        LogLine moved = new LogLine(5L, Level.WARN, "Screens", "stuck", 1, null, "x.Screens", "goTo",
                "com.example.Collect", 57, Map.of("screen", "Title", "to", "Home"));
        try (RunLog log = RunLog.start(root, facts(), CLOCK)) {
            log.write(moved);
            log.write(line(Level.ERROR, "no values"));
        }
        Path file = root.resolve("20261103-101530").resolve(RunLog.FILE);
        assertTrue(Files.readAllLines(file).get(1).contains("\"v\":{"));
        RunLogs.Readable read = read(file);

        assertEquals(moved, read.lines().getFirst());
        assertTrue(read.report().dimensions().stream().anyMatch(d -> d.id().equals("plugin.screen")
                && d.label().equals("screen")));
        assertEquals("Title", read.report().occurrences().getFirst().value("plugin.screen"));
        assertEquals("Home", read.report().occurrences().getFirst().value("plugin.to"));
        assertEquals(Map.of(), read.lines().get(1).values());
        assertTrue(read.report().lines().isEmpty(), "no other line carried values");
    }

    @Test
    void anyOtherLineCarryingValuesIsInTheReportsLinesNotItsProblems() throws IOException {
        try (RunLog log = RunLog.start(root, facts(), CLOCK)) {
            log.write(new LogLine(5L, Level.INFO, "Screens", "Home: 3 visits", 1, null, "", "", "", -1,
                    Map.of("screen", "Home", "millis", "1200")));
            log.write(line(Level.INFO, "no values"));
            log.write(line(Level.ERROR, "a problem"));
        }
        RunLogs.Readable read = read(root.resolve("20261103-101530").resolve(RunLog.FILE));

        assertEquals(1, read.report().occurrences().size());
        assertEquals(1, read.report().lines().size());
        assertEquals("Home", read.report().lines().getFirst().value("plugin.screen"));
        assertEquals("1200", read.report().lines().getFirst().value("plugin.millis"));
        assertEquals("info", read.report().lines().getFirst().value("level"));
        assertEquals(1, read.report().plus(read.report()).lines().size() / 2);
    }

    @Test
    void theWriterNamesThisJarsVersion() {
        assertTrue(RunLogs.WRITER.matches("com\\.github\\.BotMakerDev:botmaker-runlog:\\d+\\.\\d+\\.\\d+.*"),
                RunLogs.WRITER);
    }

    @Test
    void tornLastLineIsSkipped() throws IOException {
        Path file;
        try (RunLog log = RunLog.start(root, facts(), CLOCK)) {
            log.write(line(Level.INFO, "whole"));
            file = log.file();
        }
        Files.writeString(file, "{\"t\":9,\"level\":\"err", StandardCharsets.UTF_8, StandardOpenOption.APPEND);
        List<LogLine> lines = read(file).lines();
        assertEquals(1, lines.size());
        assertEquals("whole", lines.getFirst().text());
    }

    @Test
    void aNewerFieldStillReads() throws IOException {
        Path file = root.resolve(RunLog.FILE);
        Files.writeString(file, """
                {"format":1,"writer":"w","facts":{"bot":"b","shoeSize":44,"details":{}},"later":true}
                {"t":3,"level":"warn","text":"x","colour":"red"}
                """);
        RunLogs.Readable read = read(file);
        assertEquals("b", read.facts().bot());
        assertEquals(RunFacts.UNKNOWN, read.facts().tag());
        assertEquals(Level.WARN, read.lines().getFirst().level());
    }

    @Test
    void fiveRunsAreKept() throws IOException {
        for (int i = 0; i < 7; i++) RunLog.start(root, facts(), CLOCK).close();
        try (Stream<Path> dirs = Files.list(root)) {
            assertEquals(RunLog.KEEP_RUNS, dirs.count());
        }
    }

    @Test
    void anOldRunWithASubfolderIsStillPruned() throws IOException {
        for (int i = 0; i < RunLog.KEEP_RUNS; i++) {
            Path folder = RunLog.newFolder(root, CLOCK);
            Files.createDirectories(folder.resolve("frames").resolve("deep"));
        }
        RunLog.start(root, facts(), CLOCK).close();
        try (Stream<Path> dirs = Files.list(root)) {
            assertEquals(RunLog.KEEP_RUNS, dirs.count());
        }
    }

    @Test
    void aFactUnknownToThePluginReadsUnknown() {
        Map<String, String> details = new HashMap<>();
        details.put("Game version", null);
        details.put(null, "x");
        RunFacts facts = new RunFacts(null, null, null, false, null, null, 0, details);
        assertEquals(Map.of("Game version", RunFacts.UNKNOWN), facts.details());
        assertEquals(RunFacts.UNKNOWN, facts.bot());
    }

    @Test
    void aLogStartedInAnExistingFolderSharesIt() throws IOException {
        Path folder = RunLog.newFolder(root, CLOCK);
        try (RunLog log = RunLog.in(folder, facts())) {
            assertEquals(folder, log.folder());
        }
        try (Stream<Path> dirs = Files.list(root)) {
            assertEquals(1, dirs.count());
        }
    }

    @Test
    void pastItsCapTheLogDropsNewDebugLinesThenEverything() throws IOException {
        String big = "x".repeat(4000);
        Path file;
        try (RunLog log = RunLog.start(root, facts(), CLOCK)) {
            file = log.file();
            while (Files.size(file) < RunLog.DEBUG_BYTES) log.write(line(Level.DEBUG, big));
            long atDebugCap = Files.size(file);
            log.write(line(Level.DEBUG, big));
            assertEquals(atDebugCap, Files.size(file), "a debug line past 4 MB is dropped");
            while (Files.size(file) < RunLog.MAX_BYTES) log.write(line(Level.WARN, big));
            log.write(line(Level.ERROR, "late"));
            long full = Files.size(file);
            log.write(line(Level.ERROR, "later"));
            assertEquals(full, Files.size(file));
        }
        List<LogLine> lines = read(file).lines();
        assertEquals("The run's log is full; later lines are not kept.", lines.getLast().text());
        assertTrue(lines.stream().noneMatch(l -> l.text().startsWith("late")));
    }
}
