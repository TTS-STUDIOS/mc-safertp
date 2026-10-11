package com.ttsstudio.safertp.cfg;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Typed wrapper around {@code config.yml}.
 */
public final class ConfigManager {

    private final JavaPlugin plugin;
    private FileConfiguration cfg;

    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        this.cfg = plugin.getConfig();
    }

    public FileConfiguration raw() { return cfg; }

    /** Active locale (ISO-639-1 two-letter code, lowercase). Default "es". */
    public String language() { return cfg.getString("language", "en").toLowerCase(); }

    /** World used by a bare /rtp when the player's current world has RTP disabled (empty = none). */
    public String defaultWorld() { return cfg.getString("default-world", ""); }

    /** /rtp without a world: the destination menu ({@code menu:}). */
    public com.ttsstudio.safertp.gui.RtpMenu.Settings menu() {
        return com.ttsstudio.safertp.gui.RtpMenu.Settings.from(cfg.getConfigurationSection("menu"));
    }

    // ─── Back ─────────────────────────────────────────────────────────────────
    public boolean backEnabled()        { return cfg.getBoolean("back.enabled", true); }
    public long    backTtlSeconds()     { return cfg.getLong("back.ttl-seconds", 300L); }
    public long    backCooldownSeconds(){ return cfg.getLong("back.cooldown-seconds", 60L); }

    // ─── Arrival effects ──────────────────────────────────────────────────────
    public int    arrivalInvulnerabilitySeconds() { return cfg.getInt("arrival.invulnerability-seconds", 5); }
    public boolean arrivalParticlesEnabled()      { return cfg.getBoolean("arrival.particles.enabled", true); }
    public String  arrivalParticleType()          { return cfg.getString("arrival.particles.type", "END_ROD"); }
    public int     arrivalParticleCount()         { return cfg.getInt("arrival.particles.count", 40); }
    public boolean arrivalSoundEnabled()          { return cfg.getBoolean("arrival.sound.enabled", true); }
    public String  arrivalSoundType()             { return cfg.getString("arrival.sound.type", "ENTITY_ENDERMAN_TELEPORT"); }
    public float   arrivalSoundVolume()           { return (float) cfg.getDouble("arrival.sound.volume", 1.0); }
    public float   arrivalSoundPitch()            { return (float) cfg.getDouble("arrival.sound.pitch", 1.2); }

    // ─── Location cache ───────────────────────────────────────────────────────
    public boolean cacheEnabled()         { return cfg.getBoolean("cache.enabled", true); }
    public int     cacheSizePerWorld()    { return cfg.getInt("cache.size-per-world", 6); }
    public int     cacheRefillThreshold() { return cfg.getInt("cache.refill-threshold", 2); }

    // ─── Biome discovery ──────────────────────────────────────────────────────
    public boolean discoveryEnabled()      { return cfg.getBoolean("discovery.enabled", true); }
    public boolean discoverySoundEnabled() { return cfg.getBoolean("discovery.sound.enabled", true); }
    public String  discoverySoundType()    { return cfg.getString("discovery.sound.type", "UI_TOAST_CHALLENGE_COMPLETE"); }
    public float   discoverySoundVolume()  { return (float) cfg.getDouble("discovery.sound.volume", 1.0); }
    public float   discoverySoundPitch()   { return (float) cfg.getDouble("discovery.sound.pitch", 1.0); }
}
