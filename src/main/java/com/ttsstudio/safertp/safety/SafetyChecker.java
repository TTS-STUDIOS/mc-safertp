package com.ttsstudio.safertp.safety;

import com.ttsstudio.safertp.config.WorldConfig;
import com.ttsstudio.safertp.integration.WorldGuardHook;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

public final class SafetyChecker {

    public static final Set<String> DANGEROUS_FLOOR = Set.of(
        "LAVA", "FIRE", "MAGMA_BLOCK", "CAMPFIRE", "SOUL_FIRE",
        "CACTUS", "SWEET_BERRY_BUSH", "WITHER_ROSE", "POWDER_SNOW"
    );

    private SafetyChecker() {}

    public static boolean isSafe(Location loc, WorldConfig config) {
        return isSafe(loc, config, null);
    }

    public static boolean isSafe(Location loc, WorldConfig config,
                                  @Nullable WorldGuardHook wgHook) {
        if (loc == null || loc.getWorld() == null) return false;

        if (!loc.getWorld().getWorldBorder().isInside(loc)) return false;

        if (config.isAvoided(loc.getBlockX(), loc.getBlockZ())) return false;

        if (wgHook != null && wgHook.isInProtectedRegion(loc)) return false;

        int y    = loc.getBlockY();
        int minY = loc.getWorld().getMinHeight() + 1;
        int maxY = loc.getWorld().getMaxHeight() - 2;
        if (y < minY || y > maxY) return false;

        Block feet  = loc.getBlock();
        Block head  = feet.getRelative(0, 1, 0);
        Block floor = feet.getRelative(0, -1, 0);

        if (!isPassable(feet.getType())) return false;
        if (!isPassable(head.getType())) return false;
        if (!floor.getType().isSolid()) return false;
        if (DANGEROUS_FLOOR.contains(floor.getType().name())) return false;

        Biome biome = loc.getWorld().getBiome(loc.getBlockX(), y, loc.getBlockZ());
        if (!config.biomeWhitelist().isEmpty() && !config.biomeWhitelist().contains(biome)) return false;
        if (config.biomeBlacklist().contains(biome)) return false;

        return true;
    }

    private static boolean isPassable(Material mat) {
        return mat == Material.AIR || mat == Material.CAVE_AIR || mat == Material.VOID_AIR;
    }
}
