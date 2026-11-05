package com.ttsstudio.safertp.config;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class FirstTripTest {

    private static WorldConfig world(int min, int max, int firstMax) {
        return new WorldConfig("world", true, min, max, 0, 0, Set.of(), Set.of(), 50, 0, firstMax);
    }

    @Test
    void zeroMeansOff() {
        WorldConfig c = world(500, 4800, 0);
        assertFalse(c.hasFirstTrip());
        assertSame(c, c.firstTrip());
    }

    @Test
    void firstTripOnlyShrinksTheMaximum() {
        WorldConfig first = world(500, 4800, 1500).firstTrip();
        assertEquals(500, first.minRadius());
        assertEquals(1500, first.maxRadius());
        assertFalse(first.hasFirstTrip(), "the first-trip copy must not shrink again");
    }

    @Test
    void aFirstRadiusBelowTheMinimumPullsTheMinimumDown() {
        WorldConfig first = world(1000, 10000, 600).firstTrip();
        assertEquals(600, first.minRadius());
        assertEquals(600, first.maxRadius());
    }

    @Test
    void aFirstRadiusAtOrAboveTheMaximumIsIgnored() {
        assertFalse(world(500, 4800, 4800).hasFirstTrip());
        assertFalse(world(500, 4800, 9000).hasFirstTrip());
    }

    @Test
    void theRegistryReadsTheKeyAndTheDefaultFileShipsIt() throws IOException {
        String reg = Files.readString(Path.of("src/main/java/com/ttsstudio/safertp/config/WorldConfigRegistry.java"),
            StandardCharsets.UTF_8);
        assertTrue(reg.contains("\"first-max-radius\""));
        String worlds = Files.readString(Path.of("src/main/resources/worlds.yml"), StandardCharsets.UTF_8);
        assertTrue(worlds.contains("first-max-radius:"));
        for (String lang : new String[]{"en", "es"}) {
            String s = Files.readString(Path.of("src/main/resources/lang/" + lang + ".yml"), StandardCharsets.UTF_8);
            assertTrue(s.contains("rtp-first-trip:"), lang + " lacks rtp-first-trip");
        }
    }
}
