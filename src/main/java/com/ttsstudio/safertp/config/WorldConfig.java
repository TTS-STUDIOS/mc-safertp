package com.ttsstudio.safertp.config;

import org.bukkit.block.Biome;
import java.util.Set;

public record WorldConfig(
    String worldName,
    boolean enabled,
    int minRadius,
    int maxRadius,
    int centerX,
    int centerZ,
    Set<Biome> biomeWhitelist,
    Set<Biome> biomeBlacklist,
    int maxAttempts,
    double cost,
    int firstMaxRadius
) {

    /** True when a player's first RTP in this world lands closer (first-max-radius > 0). */
    public boolean hasFirstTrip() {
        return firstMaxRadius > 0 && firstMaxRadius < maxRadius;
    }

    /** The same world with the radius of a player's first RTP; itself when there is none. */
    public WorldConfig firstTrip() {
        if (!hasFirstTrip()) return this;
        return new WorldConfig(worldName, enabled, Math.min(minRadius, firstMaxRadius), firstMaxRadius,
            centerX, centerZ, biomeWhitelist, biomeBlacklist, maxAttempts, cost, 0);
    }
}
