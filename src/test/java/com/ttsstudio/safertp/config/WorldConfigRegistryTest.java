package com.ttsstudio.safertp.config;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class WorldConfigRegistryTest {

    @Test
    void noSectionMeansOff() {
        assertTrue(WorldConfigRegistry.parsePortalRtp(null).isEmpty());
    }

    @Test
    void emptyWorldsMapMeansOff() {
        YamlConfiguration cfg = new YamlConfiguration();
        cfg.createSection("portal-rtp.worlds");
        Map<String, PortalRtpConfig> parsed =
            WorldConfigRegistry.parsePortalRtp(cfg.getConfigurationSection("portal-rtp.worlds"));
        assertTrue(parsed.isEmpty());
    }

    @Test
    void readsATargetAndItsCooldown() {
        YamlConfiguration cfg = new YamlConfiguration();
        cfg.set("portal-rtp.worlds.spawn.target", "world_nether");
        cfg.set("portal-rtp.worlds.spawn.cooldown-seconds", 90);

        Map<String, PortalRtpConfig> parsed =
            WorldConfigRegistry.parsePortalRtp(cfg.getConfigurationSection("portal-rtp.worlds"));

        assertEquals(1, parsed.size());
        PortalRtpConfig spawn = parsed.get("spawn");
        assertEquals("spawn", spawn.sourceWorld());
        assertEquals("world_nether", spawn.targetWorld());
        assertEquals(90L, spawn.cooldownSeconds());
    }

    @Test
    void cooldownDefaultsTo60Seconds() {
        YamlConfiguration cfg = new YamlConfiguration();
        cfg.set("portal-rtp.worlds.spawn.target", "world_nether");

        Map<String, PortalRtpConfig> parsed =
            WorldConfigRegistry.parsePortalRtp(cfg.getConfigurationSection("portal-rtp.worlds"));

        assertEquals(60L, parsed.get("spawn").cooldownSeconds());
    }

    @Test
    void anEntryWithNoTargetIsSkipped() {
        YamlConfiguration cfg = new YamlConfiguration();
        cfg.set("portal-rtp.worlds.spawn.cooldown-seconds", 30);

        Map<String, PortalRtpConfig> parsed =
            WorldConfigRegistry.parsePortalRtp(cfg.getConfigurationSection("portal-rtp.worlds"));

        assertTrue(parsed.isEmpty());
    }

    @Test
    void severalSourceWorldsCanTargetDifferentNethers() {
        YamlConfiguration cfg = new YamlConfiguration();
        cfg.set("portal-rtp.worlds.spawn.target", "world_nether");
        cfg.set("portal-rtp.worlds.lobby.target", "lobby_nether");

        Map<String, PortalRtpConfig> parsed =
            WorldConfigRegistry.parsePortalRtp(cfg.getConfigurationSection("portal-rtp.worlds"));

        assertEquals(2, parsed.size());
        assertEquals("world_nether", parsed.get("spawn").targetWorld());
        assertEquals("lobby_nether", parsed.get("lobby").targetWorld());
    }
}
