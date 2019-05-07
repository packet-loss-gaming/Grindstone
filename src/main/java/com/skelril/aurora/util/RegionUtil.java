/*
 * Copyright (c) 2014 Wyatt Childers.
 *
 * All Rights Reserved
 */

package com.skelril.aurora.util;

import com.sk89q.worldedit.BlockVector;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.managers.storage.StorageException;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedPolygonalRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.skelril.aurora.economic.store.AdminStoreComponent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class RegionUtil {

    public static Stream<ProtectedRegion> getHouseStream(LocalPlayer player, RegionManager manager) {
        return manager.getRegions().entrySet().stream()
                .filter(e -> e.getValue().getOwners().contains(player))
                .filter(e -> e.getKey().endsWith("-house"))
                .map(Map.Entry::getValue);
    }

    public static List<ProtectedRegion> getHouses(LocalPlayer player, RegionManager manager) {
        return getHouseStream(player, manager).collect(Collectors.toList());
    }

    public static int sumChunks(LocalPlayer player, RegionManager manager) {
        return sumChunks(getHouseStream(player, manager));
    }

    public static int sumChunks(Stream<ProtectedRegion> regionStream) {
        return regionStream.mapToInt(RegionUtil::countChunks).sum();
    }

    public static int countChunks(ProtectedRegion region) {
        double size = -1;
        double length, width;
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
        }
        return (int) Math.ceil(size);
    }

    public static double calcChunkPrice(double chunkCount) {
        return Math.pow(chunkCount, 4) * (chunkCount / 2);
    }

    public static double calcBlockPrice(ProtectedRegion region, World world) {

        Map<BaseBlock, Integer> blockMapping = new HashMap<>();

        double bp = 0;
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
                        BaseBlock b = world.getBlock(pt);
                        Integer count = blockMapping.get(b);
                        if (count != null) {
                            count++;
                        } else {
                            count = 1;
                        }
                        blockMapping.put(b, count);
                    }
                }
            }

            for (Map.Entry<BaseBlock, Integer> entry : blockMapping.entrySet()) {
                BaseBlock b = entry.getKey();
                bp += AdminStoreComponent.priceCheck(
                        b.getId(),
                        b.getData()
                ) * entry.getValue();
            }
        } else {
            bp = -1;
        }
        return bp;
    }

    // TODO Some more optimization
    public static double getPrice(ProtectedRegion region, World world, boolean commission) {

        double size = countChunks(region);

        if (size == -1) {
            return -1;
        }

        double total = calcChunkPrice(size) + calcBlockPrice(region, world);
        if (commission) {
            total *= 1.1;
        }

        return total;
    }

    public static boolean renameRegion(RegionManager manager, String oldName, String newName, boolean cleanPersonal)
        throws ProtectedRegion.CircularInheritanceException, StorageException {

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
