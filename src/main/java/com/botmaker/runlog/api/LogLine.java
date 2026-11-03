package com.botmaker.runlog.api;

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
 */
public record LogLine(long atMillis, Level level, String source, String text, int count, Rect rect,
                      String writerClass, String writerMethod, String className, int line) {

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
    }
}
