package com.botmaker.runlog.api;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * One line of a run's log, as the writer takes it and a reader gives it back.
 *
 * @param atMillis     when it was said
 * @param level        its level
 * @param source       who said it, a short name ({@code "Vision"})
 * @param text         what was said
 * @param count        how many times in a row, 1 for once
 * @param rect         the screen area it is about, or null
 * @param writerClass  the class that said it, or empty
 * @param writerMethod the method that said it, or empty
 * @param className    the bot's class whose call led to it, or empty
 * @param line         that class's line, or -1
 * @param values       what the plugin that said it knew of the moment, by keys of its own ({@code screen}); each is
 *                     a dimension {@code plugin.<key>} of the run's report. Never null, in the order given
 */
public record LogLine(long atMillis, Level level, String source, String text, int count, Rect rect,
                      String writerClass, String writerMethod, String className, int line,
                      Map<String, String> values) {

    /** A line carrying no plugin values. */
    public LogLine(long atMillis, Level level, String source, String text, int count, Rect rect,
                   String writerClass, String writerMethod, String className, int line) {
        this(atMillis, level, source, text, count, rect, writerClass, writerMethod, className, line, Map.of());
    }

    /** A screen area, in the game's pixels. */
    public record Rect(int x, int y, int width, int height) {
    }

    public LogLine {
        level = level == null ? Level.UNKNOWN : level;
        source = source == null ? "" : source;
        text = text == null ? "" : text;
        count = Math.max(1, count);
        writerClass = writerClass == null ? "" : writerClass;
        writerMethod = writerMethod == null ? "" : writerMethod;
        className = className == null ? "" : className;
        values = kept(values);
    }

    /** The most values one line keeps; past it the rest are dropped. */
    public static final int MAX_VALUES = 16;
    /** The longest key kept; a longer one is dropped. */
    public static final int MAX_KEY = 64;
    /** The longest value kept; a longer one is cut. */
    public static final int MAX_VALUE = 200;

    /**
     * {@code values} as a line keeps them, in order: at most {@value #MAX_VALUES}, a null or blank key, a key longer
     * than {@value #MAX_KEY} or a null value dropped, a value cut to {@value #MAX_VALUE}. What a forged issue's block
     * says goes through it too, so no reader holds more.
     */
    public static Map<String, String> kept(Map<String, String> values) {
        Map<String, String> kept = new LinkedHashMap<>();
        if (values != null) values.forEach((k, v) -> {
            if (kept.size() < MAX_VALUES && k != null && !k.isBlank() && k.length() <= MAX_KEY && v != null) {
                kept.put(k, v.length() > MAX_VALUE ? v.substring(0, MAX_VALUE) : v);
            }
        });
        return Collections.unmodifiableMap(kept);
    }
}
