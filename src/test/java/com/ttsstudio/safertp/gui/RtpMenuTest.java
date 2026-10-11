package com.ttsstudio.safertp.gui;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RtpMenuTest {

    private static RtpMenu.Entry entry(String world, int slot) {
        return new RtpMenu.Entry(world, world, slot, "GRASS_BLOCK", 0, 0, 100, 1000);
    }

    @Test
    void worldsGoToTheirSlots() {
        Map<Integer, RtpMenu.Entry> placed = RtpMenu.layout(List.of(entry("world", 11), entry("world_nether", 13)), 3);
        assertEquals("world", placed.get(11).world());
        assertEquals("world_nether", placed.get(13).world());
    }

    @Test
    void aWorldWithoutSlotTakesAFreeMiddleSlot() {
        Map<Integer, RtpMenu.Entry> placed = RtpMenu.layout(List.of(entry("world", 10), entry("mining", -1)), 3);
        assertEquals("world", placed.get(10).world());
        assertEquals("mining", placed.get(11).world());
    }

    @Test
    void aSlotOutsideTheWindowIsMovedInside() {
        Map<Integer, RtpMenu.Entry> placed = RtpMenu.layout(List.of(entry("world", 40)), 3);
        assertEquals(1, placed.size());
        assertTrue(placed.keySet().iterator().next() < 27);
    }

    @Test
    void settingsReadFromConfig() throws Exception {
        YamlConfiguration cfg = new YamlConfiguration();
        cfg.loadFromString("menu:\n  enabled: false\n  rows: 4\n  title: \"<white>\\uE030\"\n  slots:\n    world: 20\n  models:\n    world: \"tiamat:menu/rtp_overworld\"\n");
        RtpMenu.Settings s = RtpMenu.Settings.from(cfg.getConfigurationSection("menu"));
        assertFalse(s.enabled());
        assertEquals(4, s.rows());
        assertEquals(20, s.slots().get("world"));
        assertEquals(13, s.slots().get("world_nether"));
        assertEquals("<white>", s.title());
        assertEquals("tiamat:menu/rtp_overworld", s.models().get("world"));
    }

    @Test
    void withoutAMenuSectionTheMenuIsOnWithDefaults() {
        RtpMenu.Settings s = RtpMenu.Settings.from(null);
        assertTrue(s.enabled());
        assertEquals(3, s.rows());
        assertEquals(11, s.slots().get("world"));
    }
}
