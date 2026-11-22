package com.ttsstudio.safertp.safety;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DangerousMaterialsTest {

    @Test
    void lavaIsInDangerousSet() {
        assertTrue(SafetyChecker.DANGEROUS_FLOOR.contains("LAVA"));
    }

    @Test
    void fireIsInDangerousSet() {
        assertTrue(SafetyChecker.DANGEROUS_FLOOR.contains("FIRE"));
    }

    @Test
    void magmaIsInDangerousSet() {
        assertTrue(SafetyChecker.DANGEROUS_FLOOR.contains("MAGMA_BLOCK"));
    }

    @Test
    void cactusIsInDangerousSet() {
        assertTrue(SafetyChecker.DANGEROUS_FLOOR.contains("CACTUS"));
    }

    @Test
    void powderSnowIsInDangerousSet() {
        assertTrue(SafetyChecker.DANGEROUS_FLOOR.contains("POWDER_SNOW"));
    }

    @Test
    void stoneIsNotDangerous() {
        assertFalse(SafetyChecker.DANGEROUS_FLOOR.contains("STONE"));
    }

    @Test
    void grassIsNotDangerous() {
        assertFalse(SafetyChecker.DANGEROUS_FLOOR.contains("GRASS_BLOCK"));
    }

    @Test
    void setHasExpectedSize() {
        assertEquals(9, SafetyChecker.DANGEROUS_FLOOR.size());
    }

    @Test
    void dangerousFloorSet_hasExpectedSize() {
        assertEquals(9, SafetyChecker.DANGEROUS_FLOOR.size());
    }

    @Test
    void neighbourSetCoversLavaFireCactus() {
        for (String n : new String[]{"LAVA", "FIRE", "SOUL_FIRE", "CACTUS", "MAGMA_BLOCK"})
            assertTrue(SafetyChecker.DANGEROUS_NEIGHBOUR.contains(n), n);
        assertFalse(SafetyChecker.DANGEROUS_NEIGHBOUR.contains("STONE"));
    }
}
