package com.ttsstudio.safertp.command;

import java.util.Locale;

/** Minimum played hours per destination world (min-playtime-hours.&lt;world&gt; in config.yml). */
public final class PlaytimeGate {

    private static final double TICKS_PER_HOUR = 72_000.0;

    private PlaytimeGate() {
    }

    /** Hours still missing; 0 when the requirement is met or off (required &lt;= 0). */
    public static double missingHours(long playedTicks, double requiredHours) {
        if (requiredHours <= 0) return 0;
        return Math.max(0, requiredHours - playedTicks / TICKS_PER_HOUR);
    }

    /** Rounded up to one decimal so "0.0 h" is never shown while something is missing. */
    public static String format(double hours) {
        double up = Math.ceil(hours * 10) / 10.0;
        return up == Math.rint(up) ? String.valueOf((long) up) : String.format(Locale.ROOT, "%.1f", up);
    }
}
