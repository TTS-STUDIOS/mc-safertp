package com.ttsstudio.safertp.config;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class AvoidZonesTest {

    private static WorldConfig world(List<WorldConfig.AvoidZone> zones) {
        return new WorldConfig("world", true, 0, 1000, 0, 0, Set.of(), Set.of(), 50, 0, 0, zones);
    }

    @Test
    void noZonesAvoidsNothing() {
        assertFalse(world(List.of()).isAvoided(0, 0));
    }

    @Test
    void circleIncludesItsEdgeAndExcludesOutside() {
        WorldConfig c = world(List.of(new WorldConfig.AvoidZone(100, -100, 50)));
        assertTrue(c.isAvoided(100, -100));
        assertTrue(c.isAvoided(150, -100));
        assertFalse(c.isAvoided(151, -100));
        assertFalse(c.isAvoided(140, -140));
    }

    @Test
    void shortConstructorAndFirstTripKeepZones() {
        WorldConfig c = new WorldConfig("w", true, 0, 5000, 0, 0, Set.of(), Set.of(), 50, 0, 1000);
        assertFalse(c.isAvoided(0, 0));
        WorldConfig z = new WorldConfig("w", true, 0, 5000, 0, 0, Set.of(), Set.of(), 50, 0, 1000,
            List.of(new WorldConfig.AvoidZone(0, 0, 10)));
        assertTrue(z.firstTrip().isAvoided(5, 5));
    }

    @Test
    void parseSkipsIncompleteEntries() {
        List<WorldConfig.AvoidZone> zones = WorldConfigRegistry.parseAvoidZones(List.of(
            Map.of("x", 1, "z", 2, "radius", 30),
            Map.of("x", 1, "z", 2),
            Map.of("x", 1, "z", 2, "radius", 0),
            Map.of("x", "a", "z", 2, "radius", 5)));
        assertEquals(List.of(new WorldConfig.AvoidZone(1, 2, 30)), zones);
    }
}
