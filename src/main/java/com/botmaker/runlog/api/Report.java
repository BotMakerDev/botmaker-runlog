package com.botmaker.runlog.api;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Problems, whatever format they were read from: what they can be grouped by and each occurrence. Reports from
 * several runs or issues are {@linkplain #plus added}.
 *
 * @param dimensions  what occurrences can be grouped by
 * @param occurrences the warnings and errors, what is counted unless a reader asks for more
 * @param lines       every other line that carried a plugin's values (a {@code plugin.<key>} dimension), for a view
 *                    a plugin declares over all its lines; only a run's own log has them, an issue none
 */
public record Report(List<Dimension> dimensions, List<Occurrence> occurrences, List<Occurrence> lines) {

    public static final Report EMPTY = new Report(List.of(), List.of());

    /** Problems only, no other line. */
    public Report(List<Dimension> dimensions, List<Occurrence> occurrences) {
        this(dimensions, occurrences, List.of());
    }

    public Report {
        dimensions = List.copyOf(dimensions);
        occurrences = List.copyOf(occurrences);
        lines = lines == null ? List.of() : List.copyOf(lines);
    }

    /** Both reports' occurrences and lines; each dimension once, this one's first. */
    public Report plus(Report other) {
        Map<String, Dimension> dims = new LinkedHashMap<>();
        dimensions.forEach(d -> dims.putIfAbsent(d.id(), d));
        other.dimensions.forEach(d -> dims.putIfAbsent(d.id(), d));
        List<Occurrence> all = new ArrayList<>(occurrences);
        all.addAll(other.occurrences);
        List<Occurrence> more = new ArrayList<>(lines);
        more.addAll(other.lines);
        return new Report(List.copyOf(dims.values()), all, more);
    }
}
