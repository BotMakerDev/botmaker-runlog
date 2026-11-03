package com.botmaker.runlog;

import com.botmaker.runlog.api.LogLine;
import com.botmaker.runlog.api.Report;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Properties;

/**
 * Reads a run's log, whatever format wrote it: <b>every format's reader is in this jar</b> and none is ever deleted
 * (spec {@code docs/superpowers/specs/2026-11-03-run-reports-design.md} §6, the user's 2026-11-03 call: no SPI,
 * nothing loaded at run time). The log's first line names its {@code format}; a log from phase 1 of the plan names
 * none and is format 1.
 *
 * <p>A new format — a field renamed or changed in meaning; an added field is none — adds {@code FormatN} beside
 * {@link Format1}, a case in {@link #read}, and a fixture under {@code src/test/resources/formats/N/} kept forever.
 */
public final class RunLogs {

    /** The format this jar writes. */
    public static final int FORMAT = 1;
    /** This jar's coordinate and version, as every log it writes names it. */
    public static final String WRITER = "com.github.BotMakerDev:botmaker-runlog:" + version();

    private RunLogs() {
    }

    /** What reading a log gave. */
    public sealed interface Read {
    }

    /**
     * A log read.
     *
     * @param format the format it was written in
     * @param writer the coordinate that wrote it, or {@value RunFacts#UNKNOWN}
     * @param facts  its run's facts
     * @param lines  its lines, in order, without the facts
     * @param report its problems
     */
    public record Readable(int format, String writer, RunFacts facts, List<LogLine> lines, Report report)
            implements Read {
        public Readable {
            lines = List.copyOf(lines);
        }
    }

    /** A log or an issue that could not be read, and why, in words a person reads. */
    public record Unreadable(String reason) implements Read, IssueRead {
    }

    /** What reading an issue gave. */
    public sealed interface IssueRead {
    }

    /**
     * An issue read: its problem's occurrences, the body's dated when the issue was opened, each comment
     * {@link IssueText#comment} wrote dated when it was made.
     *
     * @param format      the format its machine block names
     * @param writer      the coordinate that wrote it, or {@value RunFacts#UNKNOWN}
     * @param fingerprint its problem's fingerprint
     * @param report      its occurrences
     */
    public record ReadableIssue(int format, String writer, String fingerprint, Report report) implements IssueRead {
    }

    /**
     * Reads an issue {@link IssueText} wrote, whatever format: {@code body} opened at {@code created}, with its
     * {@code comments}. The text is a runner's to forge: a {@code format} or {@code writer} in it is a number and a
     * name, loads nothing, and a forged one costs an unreadable issue at most.
     */
    public static IssueRead readIssue(String body, Instant created, List<IssueComment> comments) {
        JsonNode block = Format1.parse(IssueText.block(body));
        if (block == null) return new Unreadable("This issue holds no BotMaker report.");
        int format = block.path("format").asInt(0);
        return switch (format) {
            case 1 -> Format1.issue(block, created, comments == null ? List.of() : comments);
            default -> format > FORMAT
                    ? new Unreadable("This issue was written by a newer BotMaker (format " + format
                            + "). Update Studio to read this log.")
                    : new Unreadable("This issue names format " + format + ", which no BotMaker wrote.");
        };
    }

    /** Reads {@code file} by the format its first line names. */
    public static Read read(Path file) {
        int format;
        try (BufferedReader in = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            JsonNode first = Format1.parse(in.readLine());
            if (first == null || !first.has("facts")) return new Unreadable("This file is not a run's log.");
            format = first.path("format").asInt(1);
        } catch (IOException e) {
            return new Unreadable("The run's log could not be read: " + e.getMessage());
        }
        return switch (format) {
            case 1 -> Format1.read(file);
            default -> format > FORMAT
                    ? new Unreadable("This log was written by a newer BotMaker (format " + format
                            + "). Update Studio to read this log.")
                    : new Unreadable("This log names format " + format + ", which no BotMaker wrote.");
        };
    }

    private static String version() {
        try (InputStream in = RunLogs.class.getResourceAsStream("runlog.properties")) {
            if (in == null) return RunFacts.UNKNOWN;
            Properties p = new Properties();
            p.load(in);
            String v = p.getProperty("version", "");
            return v.isBlank() || v.startsWith("${") ? RunFacts.UNKNOWN : v;
        } catch (IOException e) {
            return RunFacts.UNKNOWN;
        }
    }
}
