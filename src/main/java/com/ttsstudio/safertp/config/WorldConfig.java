package com.ttsstudio.safertp.config;

import org.bukkit.block.Biome;
import java.util.List;
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
    int firstMaxRadius,
    List<AvoidZone> avoidZones
) {

    /** A circle (block x/z, radius) where /rtp never lands, e.g. an event area. */
    public record AvoidZone(int x, int z, int radius) {
        public boolean contains(int bx, int bz) {
            long dx = (long) bx - x, dz = (long) bz - z;
            return dx * dx + dz * dz <= (long) radius * radius;
        }
    }

    public WorldConfig(String worldName, boolean enabled, int minRadius, int maxRadius, int centerX, int centerZ,
                       Set<Biome> biomeWhitelist, Set<Biome> biomeBlacklist, int maxAttempts, double cost,
                       int firstMaxRadius) {
        this(worldName, enabled, minRadius, maxRadius, centerX, centerZ, biomeWhitelist, biomeBlacklist,
            maxAttempts, cost, firstMaxRadius, List.of());
    }

    /** True when (bx, bz) falls inside any avoid zone. */
    public boolean isAvoided(int bx, int bz) {
        for (AvoidZone z : avoidZones) if (z.contains(bx, bz)) return true;
        return false;
    }

    /** True when one avoid zone swallows the whole ring, so /rtp can never find a spot. */
    public boolean ringFullyAvoided() {
        return discFullyAvoided(maxRadius);
    }

    /** True when first-max-radius is set but one avoid zone swallows that smaller disc; first trips then use max-radius. */
    public boolean firstRingFullyAvoided() {
        return firstMaxRadius > 0 && firstMaxRadius < maxRadius && discFullyAvoided(firstMaxRadius);
    }

    private boolean discFullyAvoided(int outerRadius) {
        for (AvoidZone z : avoidZones) {
            double gap = Math.hypot((double) z.x() - centerX, (double) z.z() - centerZ);
            if (gap + outerRadius <= z.radius()) return true;
        }
        return false;
    }

    /** True when the ring's centre sits inside an avoid zone (a min-radius 0 world then loses its middle). */
    public boolean centerAvoided() {
        return isAvoided(centerX, centerZ);
    }

    /** True when a player's first RTP in this world lands closer (first-max-radius > 0). */
    public boolean hasFirstTrip() {
        return firstMaxRadius > 0 && firstMaxRadius < maxRadius && !firstRingFullyAvoided();
    }

    /** The same world with the radius of a player's first RTP; itself when there is none. */
    public WorldConfig firstTrip() {
        if (!hasFirstTrip()) return this;
        return new WorldConfig(worldName, enabled, Math.min(minRadius, firstMaxRadius), firstMaxRadius,
            centerX, centerZ, biomeWhitelist, biomeBlacklist, maxAttempts, cost, 0, avoidZones);
    }
}
