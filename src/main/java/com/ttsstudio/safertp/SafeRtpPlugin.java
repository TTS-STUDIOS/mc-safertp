package com.ttsstudio.safertp;

import com.ttsstudio.safertp.back.BackLocationStore;
import com.ttsstudio.safertp.discovery.BiomeDiscoveryTracker;
import com.ttsstudio.safertp.cfg.ConfigManager;
import com.ttsstudio.safertp.cfg.MessageManager;
import com.ttsstudio.safertp.command.RtpCommand;
import com.ttsstudio.safertp.config.WorldConfigRegistry;
import com.ttsstudio.safertp.finder.CacheRefiller;
import com.ttsstudio.safertp.finder.LocationCache;
import com.ttsstudio.safertp.finder.LocationFinder;
import com.ttsstudio.safertp.integration.PapiHook;
import com.ttsstudio.safertp.integration.VaultHook;
import com.ttsstudio.safertp.integration.WorldGuardHook;
import com.ttsstudio.safertp.listener.WarmupListener;
import com.ttsstudio.safertp.manager.CooldownManager;
import com.ttsstudio.safertp.manager.WarmupManager;
import com.ttsstudio.sdk.PluginIdentity;
import com.ttsstudio.sdk.compat.PluginLog;
import com.ttsstudio.sdk.console.ConsoleBanner;
import com.ttsstudio.sdk.text.Texts;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.Duration;

public final class SafeRtpPlugin extends JavaPlugin {

    private static final long BACK_PURGE_INTERVAL_TICKS = 20L * 60L; // every 60s

    private ConfigManager configManager;
    private final com.ttsstudio.sdk.gui.GuiClickRouter guiRouter = new com.ttsstudio.sdk.gui.GuiClickRouter();
    private MessageManager messageManager;
    private WorldConfigRegistry worldConfigRegistry;
    private CooldownManager cooldownManager;
    private WarmupManager warmupManager;
    private BackLocationStore backLocationStore;
    private VaultHook vaultHook;
    private WorldGuardHook worldGuardHook;
    private LocationCache locationCache;
    private BiomeDiscoveryTracker discoveryTracker;

    @Override
    public void onEnable() {
        Texts.install(this);
        long startTime = System.currentTimeMillis();
        configManager = new ConfigManager(this);
        configManager.reload();
        messageManager = new MessageManager(this, configManager);
        messageManager.reload();

        worldConfigRegistry = new WorldConfigRegistry(this);
        worldConfigRegistry.load();

        cooldownManager = new CooldownManager();
        warmupManager = new WarmupManager(this);

        long ttlSeconds = configManager.backTtlSeconds();
        backLocationStore = new BackLocationStore(ttlSeconds * 1000L);

        discoveryTracker = new BiomeDiscoveryTracker(getDataFolder());
        discoveryTracker.load();
        com.ttsstudio.sdk.scheduler.Scheduler.asyncTimer(this,
            t -> backLocationStore.purgeExpired(),
            BACK_PURGE_INTERVAL_TICKS, BACK_PURGE_INTERVAL_TICKS);

        locationCache = new LocationCache(
            configManager.cacheSizePerWorld(),
            configManager.cacheRefillThreshold());

        // Background cache refill — at most one new search per tick, and the searches
        // still running count towards the cache size (see CacheRefiller).
        CacheRefiller refiller = new CacheRefiller(
            () -> locationCache,
            () -> new java.util.ArrayList<>(worldConfigRegistry.all()),
            () -> configManager.cacheSizePerWorld(),
            wc -> {
                org.bukkit.World w = org.bukkit.Bukkit.getWorld(wc.worldName());
                return w == null ? null : LocationFinder.findSafe(w, wc, worldGuardHook);
            });
        // Global on Folia: it only starts searches; each search reads its chunk on that chunk's region.
        com.ttsstudio.sdk.scheduler.Scheduler.globalTimer(this, t -> {
            // Guard the whole per-tick body: an uncaught exception here would
            // make the Bukkit scheduler silently cancel this repeating task,
            // permanently disabling the cache refill for the server's lifetime.
            try {
                if (configManager.cacheEnabled()) refiller.tick();
            } catch (RuntimeException e) {
                PluginLog.of(this).warn("Location cache refill tick failed", e);
            }
        }, 20L * 60L, 1L);

        if (getServer().getPluginManager().isPluginEnabled("Vault")) {
            vaultHook = VaultHook.setup();
            if (vaultHook != null) {
                PluginLog.of(this).info("Vault economy hooked.");
            }
        }

        worldGuardHook = WorldGuardHook.setup();
        if (worldGuardHook != null) {
            PluginLog.of(this).info("WorldGuard region protection hooked.");
        }

        getServer().getPluginManager().registerEvents(new WarmupListener(this), this);
        getServer().getPluginManager().registerEvents(guiRouter, this);

        var rtpExecutor = new RtpCommand(this);
        var cmd = getCommand("rtp");
        if (cmd != null) {
            cmd.setExecutor(rtpExecutor);
        }

        if (getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new PapiHook(this).register();
            PluginLog.of(this).info("PlaceholderAPI expansion registered.");
        }

        new Metrics(this, 31364);

        ConsoleBanner.enable(this, PluginIdentity.of(this))
            .status(worldConfigRegistry.size() + " world(s)")
            .hook(vaultHook != null ? "Vault" : null)
            .hook(getServer().getPluginManager().isPluginEnabled("PlaceholderAPI") ? "PAPI" : null)
            .hook(configManager.backEnabled() ? "Back" : null)
            .hook(worldGuardHook != null ? "WorldGuard" : null)
            .ready(Duration.ofMillis(System.currentTimeMillis() - startTime))
            .emit();
    }

    @Override
    public void onDisable() {
        if (warmupManager != null) warmupManager.cancelAll();
        if (discoveryTracker != null) {
            try { discoveryTracker.save(); } catch (RuntimeException e) {
                PluginLog.of(this).error("Failed to save discoveries", e);
            }
        }
        ConsoleBanner.disable(this, PluginIdentity.of(this)).emit();
        Texts.shutdown();
    }

    public void reload() {
        configManager.reload();
        messageManager.reload();
        worldConfigRegistry.load();
        locationCache = new LocationCache(
            configManager.cacheSizePerWorld(),
            configManager.cacheRefillThreshold());
    }

    public ConfigManager getConfigManager()            { return configManager; }
    public com.ttsstudio.sdk.gui.GuiClickRouter guiRouter() { return guiRouter; }
    public MessageManager getMessages()                { return messageManager; }
    /** Back-compat alias — call sites using .getString() compile unchanged. */
    public MessageManager getMessagesConfig()          { return messageManager; }
    public WorldConfigRegistry getWorldConfigRegistry(){ return worldConfigRegistry; }
    public CooldownManager getCooldownManager()        { return cooldownManager; }
    public WarmupManager getWarmupManager()            { return warmupManager; }
    public BackLocationStore getBackLocationStore()    { return backLocationStore; }
    public VaultHook getVaultHook()                    { return vaultHook; }
    public WorldGuardHook getWorldGuardHook()          { return worldGuardHook; }
    public LocationCache getLocationCache()            { return locationCache; }
    public BiomeDiscoveryTracker getDiscoveryTracker() { return discoveryTracker; }
}
