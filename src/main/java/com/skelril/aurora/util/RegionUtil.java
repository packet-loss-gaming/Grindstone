package com.skelril.aurora.util;

import com.sk89q.worldedit.BlockVector;
import com.sk89q.worldedit.LocalWorld;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldguard.protection.databases.ProtectionDatabaseException;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedPolygonalRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.skelril.aurora.economic.store.AdminStoreComponent;

public class RegionUtil {

    // TODO Some more optimization
    public static double getPrice(ProtectedRegion region, LocalWorld world, boolean commission) {

        double size, length, width;
        if (region instanceof ProtectedCuboidRegion) {
            Vector min = region.getMinimumPoint();
            Vector max = region.getMaximumPoint();

            int minX = min.getBlockX();
            int minZ = min.getBlockZ();
            int maxX = max.getBlockX();
            int maxZ = max.getBlockZ();

            length = (maxX - minX) + 1;
            width = (maxZ - minZ) + 1;

            size = (length * width) / (16 * 16);
        } else {
            return -1;
        }
        double p1 = size <= 4 ? size * 75 : (size * 200) + (size * (size / 2) * 200);

        // Block Price
        double p2 = 0;

        //noinspection ConstantConditions
        if (region instanceof ProtectedCuboidRegion) {

            // Doing this for speed
            Vector min = region.getMinimumPoint();
            Vector max = region.getMaximumPoint();

            int minX = min.getBlockX();
            int minY = min.getBlockY();
            int minZ = min.getBlockZ();
            int maxX = max.getBlockX();
            int maxY = max.getBlockY();
            int maxZ = max.getBlockZ();

            for (int x = minX; x <= maxX; ++x) {
                for (int y = minY; y <= maxY; ++y) {
                    for (int z = minZ; z <= maxZ; ++z) {
                        Vector pt = new Vector(x, y, z);

                        p2 += AdminStoreComponent.priceCheck(world.getBlockType(pt), world.getBlockData(pt));
                    }
                }
            }
        } else {
            return -1;
        }

        double total = p1 + p2;
        if (commission) {
            total *= 1.1;
        }

        return total;
    }

    public static boolean renameRegion(RegionManager manager, String oldName, String newName, boolean cleanPersonal)
            throws ProtectionDatabaseException, ProtectedRegion.CircularInheritanceException {

        // Check for old conflicting regions
        ProtectedRegion oldRegion = manager.getRegion(oldName);
        ProtectedRegion newRegion = manager.getRegion(newName);

        if (oldRegion == null || newRegion != null) {
            return false;
        }

        // Recreate the region based on it's old values
        if (oldRegion instanceof ProtectedPolygonalRegion) {
            int minY = oldRegion.getMinimumPoint().getBlockY();
            int maxY = oldRegion.getMaximumPoint().getBlockY();
            newRegion = new ProtectedPolygonalRegion(newName, oldRegion.getPoints(), minY, maxY);
        } else if (oldRegion instanceof ProtectedCuboidRegion) {
            BlockVector min = oldRegion.getMinimumPoint();
            BlockVector max = oldRegion.getMaximumPoint();
            newRegion = new ProtectedCuboidRegion(newName, min, max);
        } else {
            return false;
        }

        // Assign the old values to the new region
        if (!cleanPersonal) {
            newRegion.setMembers(oldRegion.getMembers());
            newRegion.setOwners(oldRegion.getOwners());
            newRegion.setFlags(oldRegion.getFlags());
        }
        newRegion.setPriority(oldRegion.getPriority());
        newRegion.setParent(oldRegion.getParent());

        // Remove the old region and add the new region then proceed to attempt to save the regions
        // Save twice because databases can be funky
        manager.removeRegion(oldRegion.getId());
        manager.save();
        manager.addRegion(newRegion);
        manager.save();
        return true;
    }
}
