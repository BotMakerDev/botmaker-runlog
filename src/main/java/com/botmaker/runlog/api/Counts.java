package com.botmaker.runlog.api;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * The counting every reader of reports shares — Studio's charts, the CLI, Claude's tools — so each counts alike.
 * An occurrence weighs its {@link Occurrence#count()}.
 */
public final class Counts {

    /**
     * One value of a dimension and how often it was met.
     *
     * @param value   the dimension's value
     * @param byLevel how often, per level
     * @param total   how often in all
     */
    public record Count(String value, Map<Level, Integer> byLevel, int total) {
        public Count {
            byLevel = Map.copyOf(byLevel);
        }
    }

    /**
     * The counts of one period.
     *
     * @param start  the period's start
     * @param counts per value, as {@link #by}
     */
    public record Bucket(Instant start, List<Count> counts) {
        public Bucket {
            counts = List.copyOf(counts);
        }
    }

    private static final Comparator<Count> MOST_FIRST =
            Comparator.comparingInt(Count::total).reversed().thenComparing(Count::value);

    private Counts() {
    }

    /** Each value of {@code dimension} with how often it was met, most met first. */
    public static List<Count> by(Report report, String dimension) {
        return count(report.occurrences(), dimension);
    }

    /**
     * {@link #by} per period of {@code unit} ({@link ChronoUnit#DAYS} or smaller, in UTC), oldest first; a period
     * with nothing met is left out.
     */
    public static List<Bucket> overTime(Report report, String dimension, ChronoUnit unit) {
        Map<Instant, List<Occurrence>> periods = new TreeMap<>();
        for (Occurrence o : report.occurrences()) {
            periods.computeIfAbsent(o.time().truncatedTo(unit), k -> new ArrayList<>()).add(o);
        }
        List<Bucket> buckets = new ArrayList<>();
        periods.forEach((start, in) -> buckets.add(new Bucket(start, count(in, dimension))));
        return buckets;
    }

    /**
     * {@code a + b}, held at {@link Integer#MAX_VALUE}: counts are a runner's, and an issue's forged count must not
     * wrap to a negative total.
     */
    public static int add(int a, int b) {
        return (int) Math.max(Integer.MIN_VALUE, Math.min(Integer.MAX_VALUE, (long) a + b));
    }

    private static List<Count> count(List<Occurrence> occurrences, String dimension) {
        Map<String, Map<Level, Integer>> per = new LinkedHashMap<>();
        for (Occurrence o : occurrences) {
            per.computeIfAbsent(o.value(dimension), k -> new EnumMap<>(Level.class))
                    .merge(o.level(), o.count(), Counts::add);
        }
        List<Count> counts = new ArrayList<>();
        per.forEach((value, levels) -> counts.add(
                new Count(value, levels, levels.values().stream().reduce(0, Counts::add))));
        counts.sort(MOST_FIRST);
        return counts;
    }
}
