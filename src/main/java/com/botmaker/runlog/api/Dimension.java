package com.botmaker.runlog.api;

/**
 * Something occurrences can be grouped by, named by the format that read them: a reader of reports offers each one
 * without knowing what it means.
 *
 * @param id    the key in {@link Occurrence#values()}: {@code "botLine"}, {@code "details.Flow"}
 * @param label what a person reads: {@code "Bot line"}, {@code "Flow"}
 */
public record Dimension(String id, String label) {

    /** What the id of a dimension a plugin's line value gives starts with: {@code plugin.screen}. */
    public static final String PLUGIN = "plugin.";

    /** The id of the dimension a plugin's line value under {@code key} gives. */
    public static String plugin(String key) {
        return PLUGIN + key;
    }
}
