package com.botmaker.runlog.api;

/**
 * A line's level, by who handled what it tells: {@link #DEBUG} and {@link #INFO} the bot's code handled it,
 * {@link #WARN} the plugin's fallback rescued the run, {@link #ERROR} nobody did. Only the last two are problems.
 */
public enum Level {
    DEBUG("debug", "Debug"),
    INFO("info", "Info"),
    WARN("warn", "Warning"),
    ERROR("error", "Error"),
    /** A level this reader does not know: written by a newer one. */
    UNKNOWN("unknown", "Unknown");

    private final String id;
    private final String displayName;

    Level(String id, String displayName) {
        this.id = id;
        this.displayName = displayName;
    }

    /** As written in a log: {@code "warn"}. */
    public String id() {
        return id;
    }

    public String displayName() {
        return displayName;
    }

    /** Whether a report counts it. */
    public boolean problem() {
        return this == WARN || this == ERROR;
    }

    /** The level {@code id} names, any case; {@link #UNKNOWN} for anything else. */
    public static Level fromId(String id) {
        if (id != null) {
            for (Level level : values()) {
                if (level.id.equalsIgnoreCase(id.strip())) return level;
            }
        }
        return UNKNOWN;
    }
}
