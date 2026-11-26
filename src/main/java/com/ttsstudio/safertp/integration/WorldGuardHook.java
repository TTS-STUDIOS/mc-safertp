package com.ttsstudio.safertp.integration;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.association.RegionAssociable;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import org.bukkit.Bukkit;
import org.bukkit.Location;

import java.util.logging.Logger;

public class WorldGuardHook {

    private static final Logger LOGGER = Logger.getLogger("SafeRTP");

    private final RegionContainer regionContainer;

    private WorldGuardHook(RegionContainer container) {
        this.regionContainer = container;
    }

    public static WorldGuardHook setup() {
        if (!Bukkit.getPluginManager().isPluginEnabled("WorldGuard")) return null;
        try {
            RegionContainer container =
                WorldGuard.getInstance().getPlatform().getRegionContainer();
            return new WorldGuardHook(container);
        } catch (LinkageError e) {
            // WorldGuard is present but its classes can't be linked (version
            // mismatch / wrong class file version). Surface it instead of
            // silently disabling region protection.
            LOGGER.severe("WorldGuard is installed but could not be linked"
                + " (version mismatch?). Region protection is DISABLED: " + e);
            return null;
        } catch (RuntimeException e) {
            LOGGER.warning("Failed to hook WorldGuard; region protection is"
                + " disabled: " + e);
            return null;
        }
    }

    public boolean isPvpDenied(Location loc) {
        if (loc.getWorld() == null) return false;
        return regionContainer.createQuery().queryState(
            BukkitAdapter.adapt(loc), (RegionAssociable) null, Flags.PVP) == StateFlag.State.DENY;
    }

    public boolean isInProtectedRegion(Location loc) {
        if (loc.getWorld() == null) return false;
        RegionManager regions = regionContainer.get(BukkitAdapter.adapt(loc.getWorld()));
        if (regions == null) return false;
        return !regions.getApplicableRegions(
            BukkitAdapter.asBlockVector(loc)).getRegions().isEmpty();
    }
}
