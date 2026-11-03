package com.botmaker.runlog.api;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Counting by a dimension, stacked by level, and over time; an occurrence weighs its count. */
class CountsTest {

    private static Occurrence at(String day, Level level, String botLine, int count) {
        return new Occurrence(Instant.parse(day + "T10:00:00Z"), level, botLine + "|" + level.id(), count,
                Map.of("botLine", botLine));
    }

    private static final Report REPORT = new Report(List.of(new Dimension("botLine", "Bot line")), List.of(
            at("2026-11-01", Level.WARN, "A:1", 3),
            at("2026-11-01", Level.ERROR, "A:1", 1),
            at("2026-11-02", Level.WARN, "B:2", 1),
            at("2026-11-02", Level.WARN, "A:1", 1)));

    @Test
    void byADimensionMostMetFirstStackedByLevel() {
        List<Counts.Count> counts = Counts.by(REPORT, "botLine");
        assertEquals(List.of("A:1", "B:2"), counts.stream().map(Counts.Count::value).toList());
        assertEquals(5, counts.getFirst().total());
        assertEquals(Map.of(Level.WARN, 4, Level.ERROR, 1), counts.getFirst().byLevel());
    }

    @Test
    void aDimensionAnOccurrenceLacksCountsAsUnknown() {
        assertEquals(List.of(Occurrence.UNKNOWN),
                Counts.by(REPORT, "details.Flow").stream().map(Counts.Count::value).toList());
    }

    @Test
    void overTimeOneBucketPerDayOldestFirst() {
        List<Counts.Bucket> days = Counts.overTime(REPORT, "botLine", ChronoUnit.DAYS);
        assertEquals(List.of(Instant.parse("2026-11-01T00:00:00Z"), Instant.parse("2026-11-02T00:00:00Z")),
                days.stream().map(Counts.Bucket::start).toList());
        assertEquals(4, days.getFirst().counts().getFirst().total());
        assertEquals(2, days.getLast().counts().size());
    }

    @Test
    void reportsAddUpWithEachDimensionOnce() {
        Report other = new Report(List.of(new Dimension("botLine", "Bot line"), new Dimension("level", "Level")),
                List.of(at("2026-11-03", Level.ERROR, "C:3", 1)));
        Report both = REPORT.plus(other);
        assertEquals(List.of("botLine", "level"), both.dimensions().stream().map(Dimension::id).toList());
        assertEquals(5, both.occurrences().size());
    }
}
