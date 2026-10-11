package com.ttsstudio.safertp.command;

import com.ttsstudio.safertp.SafeRtpPlugin;
import com.ttsstudio.safertp.back.BackLocationStore;
import com.ttsstudio.safertp.config.WorldConfig;
import com.ttsstudio.safertp.discovery.BiomeDiscoveryTracker;
import com.ttsstudio.safertp.finder.LocationFinder;
import com.ttsstudio.safertp.finder.NoSafeLocationException;
import com.ttsstudio.safertp.integration.VaultHook;
import com.ttsstudio.sdk.PluginIdentity;
import com.ttsstudio.sdk.chat.ChatPrefix;
import com.ttsstudio.sdk.compat.Particles;
import com.ttsstudio.sdk.compat.PluginLog;
import com.ttsstudio.sdk.compat.Sounds;
import com.ttsstudio.sdk.text.Texts;
import io.papermc.lib.PaperLib;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import org.bukkit.Keyed;

public class RtpCommand implements CommandExecutor {

    private static final String BACK_KEY = "back";

    private static final MiniMessage MM = MiniMessage.miniMessage();
    private final SafeRtpPlugin plugin;
    private final PluginIdentity identity;
    /** Players whose search is still running: one search per player at a time. */
    private final Set<UUID> searching = ConcurrentHashMap.newKeySet();

    public RtpCommand(SafeRtpPlugin plugin) {
        this.plugin = plugin;
        this.identity = PluginIdentity.of(plugin);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        // /rtp reload
        if (args.length >= 1 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("safertp.admin")) {
                ChatPrefix.send(sender, identity, msg("rtp-no-permission"));
                return true;
            }
            plugin.reload();
            ChatPrefix.send(sender, identity, msg("reload-success"));
            return true;
        }

        // /rtp back
        if (args.length >= 1 && args[0].equalsIgnoreCase("back")) {
            return handleBack(sender);
        }

        // /rtp other <player>
        if (args.length >= 2 && args[0].equalsIgnoreCase("other")) {
            if (!sender.hasPermission("safertp.admin")) {
                ChatPrefix.send(sender, identity, msg("rtp-no-permission"));
                return true;
            }
            Player target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                ChatPrefix.send(sender, identity,
                    msg("rtp-player-not-found").replace("<player>", args[1]));
                return true;
            }
            doRtp(target, target.getWorld(), true);
            return true;
        }

        // /rtp [world]
        if (!(sender instanceof Player player)) {
            ChatPrefix.send(sender, identity, msg("players-only"));
            return true;
        }
        if (!player.hasPermission("safertp.use")) {
            ChatPrefix.send(player, identity, msg("rtp-no-permission"));
            return true;
        }

        if (args.length == 0 && plugin.getConfigManager().menu().enabled() && openMenu(player)) {
            return true;
        }

        World world = player.getWorld();
        if (args.length == 0) {
            world = resolveDefaultWorld(world);
        }
        if (args.length >= 1) {
            World requested = Bukkit.getWorld(args[0]);
            if (requested == null) {
                ChatPrefix.send(player, identity, msg("rtp-world-disabled"));
                return true;
            }
            world = requested;
        }

        String worldNode = WorldPermission.ensureRegistered(Bukkit.getPluginManager(), world.getName());
        if (!player.hasPermission(worldNode) && !player.hasPermission("safertp.admin")) {
            ChatPrefix.send(player, identity, msg("rtp-world-no-permission"));
            return true;
        }

        doRtp(player, world, false);
        return true;
    }

    /** The destination menu; false when no world can be offered (the plain /rtp runs instead). */
    private boolean openMenu(Player player) {
        var settings = plugin.getConfigManager().menu();
        long wait = player.hasPermission("safertp.bypass.cooldown") ? 0
            : Math.max(0, plugin.getCooldownManager().getRemaining(player.getUniqueId()));
        java.util.List<com.ttsstudio.safertp.gui.RtpMenu.Entry> entries = new java.util.ArrayList<>();
        for (WorldConfig c : plugin.getWorldConfigRegistry().all()) {
            World w = Bukkit.getWorld(c.worldName());
            if (!c.enabled() || w == null) continue;
            String node = WorldPermission.ensureRegistered(Bukkit.getPluginManager(), w.getName());
            if (!player.hasPermission(node) && !player.hasPermission("safertp.admin")) continue;
            entries.add(com.ttsstudio.safertp.gui.RtpMenu.entry(c, worldName(w), settings, wait));
        }
        if (entries.isEmpty()) return false;
        com.ttsstudio.safertp.gui.RtpMenu.open(plugin.guiRouter(), player, settings, entries, msg("menu-title"),
            a -> {
                java.util.Map<String, String> ph = new java.util.HashMap<>();
                for (int i = 1; i + 1 < a.length; i += 2) ph.put(a[i], a[i + 1]);
                return plugin.getMessagesConfig().get(a[0], ph);
            },
            (p, worldName) -> {
                World w = Bukkit.getWorld(worldName);
                if (w == null) {
                    ChatPrefix.send(p, identity, msg("rtp-world-disabled"));
                    return;
                }
                doRtp(p, w, false);
            });
        return true;
    }

    private boolean handleBack(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            ChatPrefix.send(sender, identity, msg("players-only"));
            return true;
        }
        if (!player.hasPermission("safertp.back")) {
            ChatPrefix.send(player, identity, msg("back-no-permission"));
            return true;
        }

        BackLocationStore store = plugin.getBackLocationStore();
        if (store == null) {
            ChatPrefix.send(player, identity, msg("back-no-location"));
            return true;
        }

        long cooldownSeconds = plugin.getConfigManager().backCooldownSeconds();
        if (!player.hasPermission("safertp.back.nocooldown")
                && plugin.getCooldownManager().isOnCooldown(player.getUniqueId(), BACK_KEY)) {
            long left = plugin.getCooldownManager()
                .getRemainingSeconds(player.getUniqueId(), BACK_KEY);
            ChatPrefix.send(player, identity,
                msg("back-on-cooldown").replace("{seconds}", String.valueOf(left)));
            return true;
        }

        Optional<Location> prev = store.get(player.getUniqueId());
        if (prev.isEmpty()) {
            ChatPrefix.send(player, identity, msg("back-no-location"));
            return true;
        }

        PaperLib.teleportAsync(player, prev.get()).thenAccept(success -> {
            if (!success) return;
            store.clear(player.getUniqueId());
            if (!player.hasPermission("safertp.back.nocooldown") && cooldownSeconds > 0) {
                plugin.getCooldownManager()
                    .setCooldown(player.getUniqueId(), BACK_KEY, cooldownSeconds);
            }
            ChatPrefix.send(player, identity, msg("back-success"));
        });
        return true;
    }

    /** Bare /rtp from a world without RTP (a spawn/lobby world) falls back to config default-world. */
    private World resolveDefaultWorld(World current) {
        var cfg = plugin.getWorldConfigRegistry().get(current.getName());
        if (cfg.isPresent() && cfg.get().enabled()) return current;
        String name = plugin.getConfigManager().defaultWorld();
        if (name == null || name.isBlank()) return current;
        World fallback = Bukkit.getWorld(name);
        return fallback != null ? fallback : current;
    }

    private void doRtp(Player player, World world, boolean bypassCooldown) {
        var optConfig = plugin.getWorldConfigRegistry().get(world.getName());
        if (optConfig.isEmpty() || !optConfig.get().enabled()) {
            ChatPrefix.send(player, identity, msg("rtp-world-disabled"));
            return;
        }
        WorldConfig config = optConfig.get();

        if (searching.contains(player.getUniqueId())) {
            Texts.actionBar(player, MM.deserialize(msg("rtp-searching")));
            return;
        }

        // Cooldown check
        if (!bypassCooldown && !player.hasPermission("safertp.bypass.cooldown")) {
            long remaining = plugin.getCooldownManager().getRemaining(player.getUniqueId());
            if (remaining > 0) {
                ChatPrefix.send(player, identity,
                    msg("rtp-cooldown").replace("<seconds>", String.valueOf(remaining)));
                return;
            }
        }

        // Cost pre-check
        VaultHook vault = plugin.getVaultHook();
        if (config.cost() > 0 && vault != null && !player.hasPermission("safertp.bypass.cost")) {
            if (!vault.has(player, config.cost())) {
                ChatPrefix.send(player, identity,
                    msg("rtp-not-enough-money").replace("<amount>",
                        String.format(java.util.Locale.ROOT, "%.2f", config.cost())));
                return;
            }
        }

        // Warmup → search → teleport
        final World finalWorld = world;
        plugin.getWarmupManager().start(player,
            plugin.getWorldConfigRegistry().getWarmupSeconds(), () -> {

                Optional<Location> cached = plugin.getLocationCache() != null
                        && plugin.getConfigManager().cacheEnabled()
                    ? plugin.getLocationCache().poll(finalWorld.getName())
                    : Optional.empty();

                CompletableFuture<Location> locationFuture;
                if (cached.isPresent()) {
                    locationFuture = CompletableFuture.completedFuture(cached.get());
                } else {
                    if (!searching.add(player.getUniqueId())) {
                        Texts.actionBar(player, MM.deserialize(msg("rtp-searching")));
                        return;
                    }
                    Texts.actionBar(player, MM.deserialize(msg("rtp-searching")));
                    locationFuture = LocationFinder.findSafe(finalWorld, config, plugin.getWorldGuardHook());
                    locationFuture.whenComplete((loc, ex) -> searching.remove(player.getUniqueId()));
                }

                locationFuture.thenAccept(loc -> {
                    // Capture pre-teleport location so /rtp back can undo this jump.
                    BackLocationStore store = plugin.getBackLocationStore();
                    if (store != null && plugin.getConfigManager().backEnabled()) {
                        store.capture(player.getUniqueId(), player.getLocation());
                    }

                    PaperLib.teleportAsync(player, loc).thenAccept(success -> {
                        if (!success) {
                            // Roll back the back-capture if the teleport actually failed.
                            if (store != null) {
                                store.clear(player.getUniqueId());
                            }
                            return;
                        }

                        // Withdraw cost
                        if (config.cost() > 0 && vault != null
                                && !player.hasPermission("safertp.bypass.cost")) {
                            vault.withdraw(player, config.cost());
                            ChatPrefix.send(player, identity,
                                msg("rtp-cost").replace("<amount>",
                                    String.format(java.util.Locale.ROOT, "%.2f", config.cost())));
                        }

                        // Apply cooldown
                        if (!bypassCooldown) {
                            int secs = plugin.getCooldownManager().resolvePlayerCooldown(
                                player, plugin.getWorldConfigRegistry().getDefaultCooldown());
                            if (secs > 0) {
                                plugin.getCooldownManager().setCooldown(player.getUniqueId(), secs);
                            }
                        }

                        // Arrival effects (invulnerability, particles, sound)
                        applyArrivalEffects(player, loc);

                        // Biome discovery notification
                        checkBiomeDiscovery(player, loc);

                        ChatPrefix.send(player, identity,
                            msg("rtp-success")
                                .replace("<world>", worldName(finalWorld))
                                .replace("<x>", String.valueOf(loc.getBlockX()))
                                .replace("<y>", String.valueOf(loc.getBlockY()))
                                .replace("<z>", String.valueOf(loc.getBlockZ())));
                    });
                }).exceptionally(ex -> {
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    if (cause instanceof NoSafeLocationException) {
                        ChatPrefix.send(player, identity, msg("rtp-no-safe-location"));
                    } else {
                        PluginLog.of(plugin).error("RTP search error", ex);
                    }
                    return null;
                });
            });
    }

    private void applyArrivalEffects(Player player, Location loc) {
        int invuln = plugin.getConfigManager().arrivalInvulnerabilitySeconds();
        if (invuln > 0) {
            player.setNoDamageTicks(invuln * 20);
        }

        if (plugin.getConfigManager().arrivalParticlesEnabled()) {
            if (!Particles.spawn(loc.getWorld(), loc.clone().add(0, 1, 0),
                    plugin.getConfigManager().arrivalParticleCount(),
                    0.5, 0.8, 0.5, 0.05, plugin.getConfigManager().arrivalParticleType())) {
                PluginLog.of(plugin).warn("Invalid arrival particle type: {}",
                    plugin.getConfigManager().arrivalParticleType());
            }
        }

        if (plugin.getConfigManager().arrivalSoundEnabled()) {
            String sound = plugin.getConfigManager().arrivalSoundType();
            if (!Sounds.exists(sound)) {
                PluginLog.of(plugin).warn("Invalid arrival sound type: {}", sound);
            } else {
                for (Player near : loc.getWorld().getPlayers()) {
                    if (near.getLocation().distanceSquared(loc) > 32 * 32) continue;
                    Sounds.play(near, loc, sound,
                        plugin.getConfigManager().arrivalSoundVolume(),
                        plugin.getConfigManager().arrivalSoundPitch());
                }
            }
        }
    }

    private void checkBiomeDiscovery(Player player, Location loc) {
        if (!plugin.getConfigManager().discoveryEnabled()) return;
        BiomeDiscoveryTracker tracker = plugin.getDiscoveryTracker();
        if (tracker == null) return;

        NamespacedKey key = ((Keyed) loc.getWorld().getBiome(loc)).getKey();
        String biomeName = formatBiomeName(key.getKey());

        if (!tracker.discover(player.getUniqueId(), biomeName)) return;

        TagResolver biome = Placeholder.component("biome", biomeComponent(key, biomeName));
        String titleStr    = plugin.getMessagesConfig().getString("discovery-title", "<gold>New Discovery!");
        String subtitleStr = plugin.getMessagesConfig().getString("discovery-subtitle", "<yellow><biome>");
        String chatStr     = plugin.getMessagesConfig().getString("discovery-chat", "New biome: <biome>");

        Texts.title(player, MM.deserialize(titleStr), MM.deserialize(subtitleStr, biome), 10, 60, 10);
        Texts.send(player, MM.deserialize(chatStr, biome));

        if (plugin.getConfigManager().discoverySoundEnabled()) {
            if (!Sounds.play(player, plugin.getConfigManager().discoverySoundType(),
                    plugin.getConfigManager().discoverySoundVolume(),
                    plugin.getConfigManager().discoverySoundPitch())) {
                PluginLog.of(plugin).warn("Invalid discovery sound: {}",
                    plugin.getConfigManager().discoverySoundType());
            }
        }

        CompletableFuture.runAsync(() -> {
            try { tracker.save(); } catch (RuntimeException e) {
                PluginLog.of(plugin).error("Failed to save discoveries", e);
            }
        });
    }

    /**
     * Biome name for the player: {@code biome-names.<key>} from the lang file if set, otherwise
     * the client's own translation (each player sees it in their game language).
     */
    private Component biomeComponent(NamespacedKey key, String fallback) {
        String custom = plugin.getMessagesConfig().optional("biome-names." + key.getKey());
        if (custom != null && !custom.isBlank()) return MM.deserialize(custom);
        return Component.translatable("biome." + key.getNamespace() + "." + key.getKey(), fallback);
    }

    private static String formatBiomeName(String key) {
        return Arrays.stream(key.split("_"))
            .map(w -> Character.toUpperCase(w.charAt(0)) + w.substring(1).toLowerCase())
            .collect(Collectors.joining(" "));
    }

    private String msg(String key) {
        return plugin.getMessagesConfig().getString(key, "<red>Missing: " + key);
    }

    /** Display name of a world: {@code world-names.<world>} from the lang file, else its id. */
    private String worldName(World world) {
        String custom = plugin.getMessagesConfig().optional("world-names." + world.getName());
        return custom != null && !custom.isBlank() ? custom : world.getName();
    }
}
