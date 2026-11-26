package com.ttsstudio.safertp.command;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PlaytimeGateTest {

    @Test
    void missingHoursCountsDownFromTicks() {
        assertEquals(4.0, PlaytimeGate.missingHours(72_000L, 5), 1e-9);
        assertEquals(0, PlaytimeGate.missingHours(72_000L * 6, 5), 1e-9);
        assertEquals(0, PlaytimeGate.missingHours(0, 0), 1e-9);
    }

    @Test
    void formatRoundsUpAndDropsZeroDecimal() {
        assertEquals("4", PlaytimeGate.format(4.0));
        assertEquals("0.1", PlaytimeGate.format(0.01));
        assertEquals("4.5", PlaytimeGate.format(4.42));
    }
}
