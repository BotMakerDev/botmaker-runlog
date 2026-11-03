package com.botmaker.runlog.api;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Problems, whatever format they were read from: what they can be grouped by and each occurrence. Reports from
 * several runs or issues are {@linkplain #plus added}.
 */
public record Report(List<Dimension> dimensions, List<Occurrence> occurrences) {

    public static final Report EMPTY = new Report(List.of(), List.of());

    public Report {
        dimensions = List.copyOf(dimensions);
        occurrences = List.copyOf(occurrences);
    }

    /** Both reports' occurrences; each dimension once, this one's first. */
    public Report plus(Report other) {
        Map<String, Dimension> dims = new LinkedHashMap<>();
        dimensions.forEach(d -> dims.putIfAbsent(d.id(), d));
        other.dimensions.forEach(d -> dims.putIfAbsent(d.id(), d));
        List<Occurrence> all = new ArrayList<>(occurrences);
        all.addAll(other.occurrences);
        return new Report(List.copyOf(dims.values()), all);
    }
}
