package com.ttsstudio.safertp.listener;

import com.ttsstudio.safertp.SafeRtpPlugin;
import com.ttsstudio.safertp.config.PortalRtpConfig;
import com.ttsstudio.safertp.config.WorldConfig;
import com.ttsstudio.safertp.finder.LocationFinder;
import com.ttsstudio.safertp.finder.NoSafeLocationException;
import com.ttsstudio.safertp.manager.CooldownManager;
import com.ttsstudio.sdk.PluginIdentity;
import com.ttsstudio.sdk.chat.ChatPrefix;
import com.ttsstudio.sdk.compat.PluginLog;
import io.papermc.lib.PaperLib;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.Optional;

/**
 * Redirects a vanilla Nether portal, for the source worlds listed in worlds.yml's
 * {@code portal-rtp.worlds} section, to a random safe spot in the configured target world
 * instead of the portal's own linked destination — which may sit outside the world border,
 * in lava, or anywhere else a fixed portal link can drift into. Off by default: a world only
 * gets this when it is listed.
 *
 * <p>The target world's own {@link WorldConfig} (radius, biome filters, max attempts) is reused
 * as-is, even when that world's {@code enabled} is false for the plain {@code /rtp} command —
 * portal-rtp has its own gate (being listed under {@code portal-rtp.worlds}).
 */
public final class PortalRtpListener implements Listener {

    public static final String COOLDOWN_KEY = "portal-rtp";

    private final SafeRtpPlugin plugin;

    public PortalRtpListener(SafeRtpPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPortal(PlayerPortalEvent event) {
        if (event.getCause() != PlayerTeleportEvent.TeleportCause.NETHER_PORTAL) return;

        World from = event.getFrom().getWorld();
        if (from == null) return;

        Optional<PortalRtpConfig> portalCfg = plugin.getWorldConfigRegistry().portalRtp(from.getName());
        if (portalCfg.isEmpty()) return;
        PortalRtpConfig redirect = portalCfg.get();

        World target = Bukkit.getWorld(redirect.targetWorld());
        Optional<WorldConfig> worldCfg = plugin.getWorldConfigRegistry().get(redirect.targetWorld());
        if (target == null || worldCfg.isEmpty()) {
            PluginLog.of(plugin).warn(
                "portal-rtp for '{}' targets '{}', which has no worlds.yml entry or isn't loaded.",
                from.getName(), redirect.targetWorld());
            return;
        }

        event.setCancelled(true);

        Player player = event.getPlayer();
        PluginIdentity identity = PluginIdentity.of(plugin);
        CooldownManager cooldowns = plugin.getCooldownManager();

        if (!player.hasPermission("safertp.bypass.cooldown")
                && cooldowns.isOnCooldown(player.getUniqueId(), COOLDOWN_KEY)) {
            long remaining = cooldowns.getRemainingSeconds(player.getUniqueId(), COOLDOWN_KEY);
            ChatPrefix.send(player, identity,
                msg("portal-rtp-cooldown").replace("<seconds>", String.valueOf(remaining)));
            return;
        }

        ChatPrefix.send(player, identity, msg("portal-rtp-searching"));
        LocationFinder.findSafe(target, worldCfg.get(), plugin.getWorldGuardHook())
            .thenAccept(loc -> PaperLib.teleportAsync(player, loc, PlayerTeleportEvent.TeleportCause.NETHER_PORTAL)
                .thenAccept(success -> {
                    if (!success) return;
                    if (!player.hasPermission("safertp.bypass.cooldown") && redirect.cooldownSeconds() > 0) {
                        cooldowns.setCooldown(player.getUniqueId(), COOLDOWN_KEY, redirect.cooldownSeconds());
                    }
                    ChatPrefix.send(player, identity,
                        msg("portal-rtp-success")
                            .replace("<x>", String.valueOf(loc.getBlockX()))
                            .replace("<y>", String.valueOf(loc.getBlockY()))
                            .replace("<z>", String.valueOf(loc.getBlockZ())));
                }))
            .exceptionally(ex -> {
                Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                if (cause instanceof NoSafeLocationException) {
                    ChatPrefix.send(player, identity, msg("portal-rtp-no-safe-location"));
                } else {
                    PluginLog.of(plugin).error("portal-rtp search error", ex);
                }
                return null;
            });
    }

    private String msg(String key) {
        return plugin.getMessagesConfig().getString(key, "<red>Missing: " + key);
    }
}
