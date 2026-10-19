package com.ttsstudio.safertp.integration;

import com.ttsstudio.safertp.SafeRtpPlugin;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PapiHook extends PlaceholderExpansion {

    private final SafeRtpPlugin plugin;

    public PapiHook(SafeRtpPlugin plugin) {
        this.plugin = plugin;
    }

    @Override public @NotNull String getIdentifier() { return "safertp"; }
    @Override public @NotNull String getAuthor()     { return "TTS-Studio"; }
    @Override public @NotNull String getVersion()    { return plugin.getDescription().getVersion(); }
    @Override public boolean persist()               { return true; }

    @Override
    public @Nullable String onPlaceholderRequest(Player player, @NotNull String params) {
        if (player == null) return "";
        return switch (params) {
            case "cooldown" -> String.valueOf(
                plugin.getCooldownManager().getRemaining(player.getUniqueId()));
            case "can_use" -> String.valueOf(
                !plugin.getCooldownManager().isOnCooldown(player.getUniqueId()));
            default -> null;
        };
    }
}
