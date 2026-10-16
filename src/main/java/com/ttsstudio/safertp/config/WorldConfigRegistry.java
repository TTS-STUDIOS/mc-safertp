package com.ttsstudio.safertp.config;

import com.ttsstudio.safertp.SafeRtpPlugin;
import com.ttsstudio.sdk.compat.PluginLog;
import org.bukkit.block.Biome;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import java.util.*;

public class WorldConfigRegistry {

    private final SafeRtpPlugin plugin;
    private final Map<String, WorldConfig> configs = new LinkedHashMap<>();
    private final Map<String, PortalRtpConfig> portalRtpConfigs = new LinkedHashMap<>();
    private int defaultCooldown = 300;
    private int warmupSeconds = 5;

    public WorldConfigRegistry(SafeRtpPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        configs.clear();
        File file = new File(plugin.getDataFolder(), "worlds.yml");
        if (!file.exists()) plugin.saveResource("worlds.yml", false);

        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        defaultCooldown = cfg.getInt("cooldown-default", 300);
        warmupSeconds   = cfg.getInt("warmup-seconds", 5);

        ConfigurationSection worlds = cfg.getConfigurationSection("worlds");
        if (worlds == null) return;

        for (String name : worlds.getKeys(false)) {
            ConfigurationSection sec = worlds.getConfigurationSection(name);
            if (sec == null) continue;

            Set<Biome> whitelist = parseBiomes(sec.getStringList("biome-whitelist"));
            Set<Biome> blacklist = parseBiomes(sec.getStringList("biome-blacklist"));

            configs.put(name, new WorldConfig(
                name,
                sec.getBoolean("enabled", true),
                sec.getInt("min-radius", 1000),
                sec.getInt("max-radius", 10000),
                sec.getInt("center-x", 0),
                sec.getInt("center-z", 0),
                whitelist,
                blacklist,
                sec.getInt("max-attempts", 50),
                sec.getDouble("cost", 0)
            ));
        }

        portalRtpConfigs.clear();
        ConfigurationSection portalRtpSec = cfg.getConfigurationSection("portal-rtp.worlds");
        portalRtpConfigs.putAll(parsePortalRtp(portalRtpSec));
        if (portalRtpSec != null) {
            for (String source : portalRtpSec.getKeys(false)) {
                if (!portalRtpConfigs.containsKey(source)) {
                    PluginLog.of(plugin).warn("portal-rtp.worlds.{} has no target world, skipping.", source);
                }
            }
        }

        PluginLog.of(plugin).info("Loaded {} world configs.", configs.size());
    }

    /** Pure parse of {@code portal-rtp.worlds}: no plugin needed, so it is unit-testable directly. */
    static Map<String, PortalRtpConfig> parsePortalRtp(ConfigurationSection worldsSection) {
        Map<String, PortalRtpConfig> result = new LinkedHashMap<>();
        if (worldsSection == null) return result;
        for (String source : worldsSection.getKeys(false)) {
            ConfigurationSection sec = worldsSection.getConfigurationSection(source);
            String target = sec == null ? null : sec.getString("target", "");
            if (target == null || target.isBlank()) continue;
            result.put(source, new PortalRtpConfig(source, target, sec.getLong("cooldown-seconds", 60L)));
        }
        return result;
    }

    private Set<Biome> parseBiomes(List<String> names) {
        Set<Biome> result = new HashSet<>();
        for (String name : names) {
            // Through the registry, not Biome.valueOf: Biome stopped being an enum in 1.21.3.
            NamespacedKey key = NamespacedKey.fromString(name.trim().toLowerCase(Locale.ROOT));
            Biome biome = key == null ? null : Registry.BIOME.get(key);
            if (biome != null) {
                result.add(biome);
            } else {
                PluginLog.of(plugin).warn("Unknown biome '{}' in worlds.yml, skipping.", name);
            }
        }
        return result;
    }

    public Optional<WorldConfig> get(String worldName) {
        return Optional.ofNullable(configs.get(worldName));
    }

    /** The portal-rtp redirect configured for a source world, if any (off by default). */
    public Optional<PortalRtpConfig> portalRtp(String sourceWorldName) {
        return Optional.ofNullable(portalRtpConfigs.get(sourceWorldName));
    }

    public java.util.Collection<WorldConfig> all() {
        return java.util.Collections.unmodifiableCollection(configs.values());
    }

    public int getDefaultCooldown() { return defaultCooldown; }
    public int getWarmupSeconds()   { return warmupSeconds; }
    public int size()               { return configs.size(); }
    public int portalRtpCount()     { return portalRtpConfigs.size(); }
}
