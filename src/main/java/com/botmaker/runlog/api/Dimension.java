package com.botmaker.runlog.api;

/**
 * Something occurrences can be grouped by, named by the format that read them: a reader of reports offers each one
 * without knowing what it means.
 *
 * @param id    the key in {@link Occurrence#values()}: {@code "botLine"}, {@code "details.Flow"}
 * @param label what a person reads: {@code "Bot line"}, {@code "Flow"}
 */
public record Dimension(String id, String label) {
}
