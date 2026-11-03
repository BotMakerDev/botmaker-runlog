package com.botmaker.runlog;

/**
 * The marker around text read off the game's screen (spec {@code 2026-11-03-run-reports-design.md} §3): a host
 * writing a call's line wraps what a screen-reading method gave in {@link #OPEN} and {@link #CLOSE}, the local log
 * keeps it as it is, and {@link Scrubber} turns each marked span into {@code …} before any text leaves the machine.
 * Nothing guesses which text came off the screen: what the plugin marks is hidden, nothing else.
 *
 * <p>The two characters are Unicode's first-strong and pop directional isolates: a reader showing the line sees the
 * text unchanged, and no text a bot types holds them.
 */
public final class ScreenText {

    /** Opens a span of screen text. */
    public static final char OPEN = '⁨';
    /** Closes it. */
    public static final char CLOSE = '⁩';
    /** What a hidden span reads as. */
    public static final String HIDDEN = "…";

    private ScreenText() {
    }

    /** {@code text} marked as read off the screen. */
    public static String mark(String text) {
        // A mark the screen itself showed is dropped, so it cannot close the span early.
        String inner = text == null ? "null" : text.replace(String.valueOf(OPEN), "").replace(String.valueOf(CLOSE), "");
        return OPEN + inner + CLOSE;
    }

    /** {@code text} with each marked span, markers included, as {@link #HIDDEN}; an unclosed one to the end. */
    public static String hide(String text) {
        if (text == null || text.indexOf(OPEN) < 0) return text;
        StringBuilder out = new StringBuilder(text.length());
        int depth = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == OPEN) {
                if (depth++ == 0) out.append(HIDDEN);
            } else if (c == CLOSE && depth > 0) {
                depth--;
            } else if (depth == 0) {
                out.append(c);
            }
        }
        return out.toString();
    }
}
