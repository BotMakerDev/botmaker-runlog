package com.botmaker.runlog;

import com.botmaker.runlog.api.Dimension;
import com.botmaker.runlog.api.Level;
import com.botmaker.runlog.api.LogLine;
import com.botmaker.runlog.api.Occurrence;
import com.botmaker.runlog.api.Report;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Format 1, the first: {@code {"format":1,"writer":…,"facts":{…}}}, then one object per line with {@code t}, {@code
 * level}, {@code source}, {@code text} and, when they say something, {@code count}, {@code rect} ({@code [x,y,w,h]}),
 * {@code writerClass}, {@code writerMethod}, {@code class} and {@code line}. Kept as it is for good: a log written in
 * it reads through it whatever format follows.
 *
 * <p>Tolerant both ways: a field it does not know is ignored, a field missing reads as unknown, so a log a newer
 * writer of the same format added fields to still reads.
 */
final class Format1 {

    static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    static final String BOT_LINE = "botLine";
    static final String METHOD = "method";
    static final String LEVEL = "level";
    static final String BOT_VERSION = "botVersion";
    static final String DETAILS = "details.";

    private Format1() {
    }

    static RunLogs.Read read(Path file) {
        List<LogLine> lines = new ArrayList<>();
        RunFacts facts = null;
        String writer = RunFacts.UNKNOWN;
        try (BufferedReader in = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            for (String text = in.readLine(); text != null; text = in.readLine()) {
                JsonNode node = parse(text);
                if (node == null) continue;
                if (node.has("facts")) {
                    facts = facts(node.get("facts"));
                    writer = node.path("writer").asText(RunFacts.UNKNOWN);
                } else {
                    lines.add(line(node));
                }
            }
        } catch (IOException ignored) {
            // what was read stands
        }
        if (facts == null) return new RunLogs.Unreadable("This file is not a run's log.");
        return new RunLogs.Readable(1, writer, facts, lines, report(facts, lines));
    }

    /** The problems among {@code lines}: each warning and error, with what it can be grouped by. */
    static Report report(RunFacts facts, List<LogLine> lines) {
        List<Dimension> dims = new ArrayList<>(List.of(new Dimension(BOT_LINE, "Bot line"),
                new Dimension(METHOD, "Called method"), new Dimension(LEVEL, "Level"),
                new Dimension(BOT_VERSION, "Bot version")));
        facts.details().keySet().forEach(k -> dims.add(new Dimension(DETAILS + k, k)));
        List<Occurrence> found = new ArrayList<>();
        for (LogLine l : lines) {
            if (!l.level().problem()) continue;
            String botLine = l.className().isEmpty() ? RunFacts.UNKNOWN
                    : l.line() < 0 ? l.className() : l.className() + ":" + l.line();
            String method = l.writerClass().isEmpty() ? RunFacts.UNKNOWN
                    : l.writerMethod().isEmpty() ? l.writerClass() : l.writerClass() + "." + l.writerMethod();
            Map<String, String> values = new HashMap<>();
            values.put(BOT_LINE, botLine);
            values.put(METHOD, method);
            values.put(LEVEL, l.level().id());
            values.put(BOT_VERSION, facts.tag());
            facts.details().forEach((k, v) -> values.put(DETAILS + k, v));
            found.add(new Occurrence(Instant.ofEpochMilli(l.atMillis()), l.level(),
                    botLine + "|" + method + "|" + l.level().id(), l.count(), values));
        }
        return new Report(dims, found);
    }

    static String json(LogLine line) {
        ObjectNode node = MAPPER.createObjectNode();
        node.put("t", line.atMillis());
        node.put("level", line.level().id());
        node.put("source", line.source());
        node.put("text", line.text());
        if (line.count() > 1) node.put("count", line.count());
        if (line.rect() != null) {
            node.putArray("rect").add(line.rect().x()).add(line.rect().y())
                    .add(line.rect().width()).add(line.rect().height());
        }
        if (!line.writerClass().isEmpty()) node.put("writerClass", line.writerClass());
        if (!line.writerMethod().isEmpty()) node.put("writerMethod", line.writerMethod());
        if (!line.className().isEmpty()) node.put("class", line.className());
        if (line.line() >= 0) node.put("line", line.line());
        return node.toString();
    }

    /** {@code text} as a JSON object, or null for anything else: a torn last line, a blank one. */
    static JsonNode parse(String text) {
        if (text == null || text.isBlank()) return null;
        try {
            JsonNode node = MAPPER.readTree(text);
            return node != null && node.isObject() ? node : null;
        } catch (IOException e) {
            return null;
        }
    }

    private static RunFacts facts(JsonNode node) {
        try {
            return MAPPER.treeToValue(node, RunFacts.class);
        } catch (IOException | IllegalArgumentException e) {
            return RunFacts.unknown(0);
        }
    }

    private static LogLine line(JsonNode node) {
        JsonNode r = node.get("rect");
        LogLine.Rect rect = r != null && r.isArray() && r.size() == 4
                ? new LogLine.Rect(r.get(0).asInt(), r.get(1).asInt(), r.get(2).asInt(), r.get(3).asInt())
                : null;
        return new LogLine(node.path("t").asLong(), Level.fromId(node.path("level").asText()),
                node.path("source").asText(), node.path("text").asText(), node.path("count").asInt(1), rect,
                node.path("writerClass").asText(), node.path("writerMethod").asText(),
                node.path("class").asText(), node.path("line").asInt(-1));
    }
}
