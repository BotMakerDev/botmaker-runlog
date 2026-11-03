package com.botmaker.runlog.api;

import java.time.Instant;
import java.util.Map;

/**
 * A problem met, once or {@code count} times in a row.
 *
 * @param time        when
 * @param level       {@link Level#WARN} or {@link Level#ERROR}
 * @param fingerprint the problem's identity: bot class and line, called class and method, level
 * @param count       how many times
 * @param values      {@link Dimension#id()} to its value here
 */
public record Occurrence(Instant time, Level level, String fingerprint, int count, Map<String, String> values) {

    /** What a dimension this occurrence has no value for reads as. */
    public static final String UNKNOWN = "unknown";

    public Occurrence {
        count = Math.max(1, count);
        values = values == null ? Map.of() : Map.copyOf(values);
    }

    /** The value of {@code dimension} here, {@value #UNKNOWN} when it has none. */
    public String value(String dimension) {
        return values.getOrDefault(dimension, UNKNOWN);
    }
}
