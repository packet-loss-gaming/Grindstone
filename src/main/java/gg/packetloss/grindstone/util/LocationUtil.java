/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.util;

import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import gg.packetloss.grindstone.util.checker.RegionChecker;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public class LocationUtil {

    public static double distanceSquared2D(Location a, Location b) {

        return Math.pow(a.getX() - b.getX(), 2) + Math.pow(a.getZ() - b.getZ(), 2);
    }

    public static Location grandBank(World world) {

        if (!world.getName().equals("City")) return null;
        return new Location(world, 592, 83, 1176.5, 180, 0);
    }

    public static boolean toGround(Player player) {
        Location target = findFreePosition(player.getLocation());
        return target != null && player.teleport(target);
    }

    public static Location findFreePosition(final Location pos) {
        return findFreePosition(pos, true);
    }

    public static Location findFreePosition(final Location pos, boolean center) {

        // Find the raw safe position
        Location mPos = findRawFreePosition(pos);

        // Null safety check
        if (mPos == null) {
            return null;
        }

        // Add the offset from the original position
        mPos.add(pos.getX() - pos.getBlockX(), 0, pos.getZ() - pos.getBlockZ());

        // Move to the center of the block if undefined
        if (center) {
            if (mPos.getX() == mPos.getBlockX()) mPos.add(.5, 0, 0);
            if (mPos.getZ() == mPos.getBlockZ()) mPos.add(0, 0, .5);
        }
        return mPos;
    }

    private static boolean isOkayBodyBlock(Block block) {
        Material type = block.getType();
        if (EnvironmentUtil.isDangerous(type)) {
            return false;
        }

        return !type.isSolid();
    }

    private static boolean isOkayStandingBlock(Block block) {
        Material type = block.getType();
        if (EnvironmentUtil.isDangerous(type)) {
            return false;
        }

        // If we're standing on a solid block, we're fine
        if (type.isSolid()) {
            return true;
        }

        // If we're standing on water, that's okay as well
        if (EnvironmentUtil.isWater(type)) {
            return true;
        }

        // Otherwise, we're standing on a non-solid block, and will fall.
        return false;
    }

    public static boolean isSafeHeadLocation(Location headLoc) {
        Block headBlock = headLoc.getBlock();

        boolean headFree = isOkayBodyBlock(headBlock);
        boolean feetFree = isOkayBodyBlock(headBlock.getRelative(BlockFace.DOWN));
        boolean safeGround = isOkayStandingBlock(headBlock.getRelative(BlockFace.DOWN, 2));

        return headFree && feetFree && safeGround;
    }

    private static Location findRawFreePositionDown(Location headLoc) {
        // Start one block higher to handle partially filled blocks better
        headLoc = headLoc.clone().add(0, 1, 0);

        while (headLoc.getBlockY() > 1) {
            // If we have found a safe head loc, return the feet position
            if (isSafeHeadLocation(headLoc)) {
                return headLoc.add(0, -1, 0);
            }

            // Move down
            headLoc.add(0, -1, 0);
        }

        return null;
    }

    private static Location findRawFreePositionUp(Location headLoc) {
        headLoc = headLoc.clone();

        while (headLoc.getBlockY() < headLoc.getWorld().getMaxHeight()) {
            // If we have found a safe head loc, return the feet position
            if (isSafeHeadLocation(headLoc)) {
                return headLoc.add(0, -1, 0);
            }

            // Move up
            headLoc.add(0, 1, 0);
        }

        return null;
    }

    public static Location findRawFreePosition(Location pos) {
        Location headLoc = new Location(
                pos.getWorld(),
                pos.getBlockX(), pos.getBlockY() + 1, pos.getBlockZ(),
                pos.getYaw(), pos.getPitch()
        );

        Location downFreePos = findRawFreePositionDown(headLoc);
        if (downFreePos != null) {
            return downFreePos;
        }

        Location upFreePos = findRawFreePositionUp(headLoc);
        if (upFreePos != null) {
            return upFreePos;
        }

        return null;
    }

    public static Location findRandomLoc(Location searchFromLocation, final int radius) {

        return findRandomLoc(searchFromLocation.getBlock(), radius);
    }

    public static Location findRandomLoc(Block searchFromLocation, final int radius) {

        return findRandomLoc(searchFromLocation, radius, false);
    }

    public static Location findRandomLoc(Location searchFromLocation, final int radius,
                                         boolean trueRandom) {

        return findRandomLoc(searchFromLocation.getBlock(), radius, trueRandom);
    }

    public static Location findRandomLoc(Location searchFromLocation, final int radius,
                                         boolean trueRandom, boolean airOnly) {

        return findRandomLoc(searchFromLocation.getBlock(), radius, trueRandom, airOnly);
    }

    public static Location findRandomLoc(Block searchFromLocation, final int radius, boolean trueRandom) {

        return findRandomLoc(searchFromLocation, radius, trueRandom, true);
    }

    public static Location findRandomLoc(Block searchFromLocation, final int radius, boolean trueRandom, boolean airOnly) {

        int trueRadius = trueRandom ? ChanceUtil.getRandom(radius) : radius;

        BlockFace dir = CollectionUtil.getElement(EnvironmentUtil.getSurroundingBlockFaces());

        if (airOnly) {
            return findFreePosition(searchFromLocation.getRelative(dir, trueRadius).getLocation());
        } else {
            return searchFromLocation.getRelative(dir, trueRadius).getLocation();
        }
    }

    public static boolean containsPlayer(World world, ProtectedRegion region) {

        for (Player player : world.getPlayers()) {
            if (isInRegion(region, player)) return true;
        }
        return false;
    }

    public static boolean isInRegion(ProtectedRegion region, Entity entity) {

        return isInRegion(region, entity.getLocation());
    }

    public static boolean isInRegion(ProtectedRegion region, Location loc) {

        return region.contains(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());

    }

    public static boolean isInRegion(World world, ProtectedRegion region, Entity entity) {

        return isInRegion(world, region, entity.getLocation());
    }

    public static boolean isInRegion(World world, ProtectedRegion region, Location loc) {

        return world.equals(loc.getWorld()) && region.contains(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());

    }

    public static List<Player> getPlayersStandingOnRegion(World world, ProtectedRegion region,
                                                          boolean includeInRegion) {

        List<Player> playerList = new ArrayList<>();
        for (Player player : world.getPlayers()) {
            Location loc = player.getLocation();
            BlockVector3 vec = BlockVector3.at(loc.getX(), loc.getY(), loc.getZ());

            if (region.contains(vec.add(0, -1, 0)) || region.contains(vec.add(0, -2, 0))) {
                playerList.add(player);
            }

            if (!includeInRegion) {
                continue;
            }

            if (region.contains(vec) || region.contains(vec.add(0, 1, 0))) {
                playerList.add(player);
            }
        }
        return playerList;
    }

    public static boolean isBelowPlayer(World world, ProtectedRegion region) {

        for (Player player : world.getPlayers()) {
            Block block = player.getLocation().getBlock();
            if (region.contains(block.getX(), block.getY() - 1, block.getZ())
                    || region.contains(block.getX(), block.getY() - 2, block.getZ())) {
                return true;
            }
        }
        return false;
    }

    public static boolean isBelowPlayer(World world, ProtectedRegion region, Player player) {

        Block block = player.getLocation().getBlock();
        return (region.contains(block.getX(), block.getY() - 1, block.getZ())
                || region.contains(block.getX(), block.getY() - 2, block.getZ()))
                && world.equals(player.getWorld());
    }

    public static boolean isCloseToPlayer(Block block, int distance) {

        return isCloseToPlayer(block.getLocation(), distance);
    }

    public static boolean isCloseToPlayer(Location location, int distance) {

        int DISTANCE_SQ = distance * distance;

        for (Player player : location.getWorld().getPlayers()) {
            if (player.getLocation().distanceSquared(location) <= DISTANCE_SQ) {
                return true;
            }
        }
        return false;
    }

    public static Location[] getNearbyLocations(Location startLocation, int searchRadius) {

        return getNearbyLocations(startLocation, searchRadius, 0);
    }

    public static Location[] getNearbyLocations(Location startLocation, int searchRadius, int vert) {

        if (searchRadius < 1) {
            return new Location[]{startLocation};
        }

        int length = BlockFace.values().length - 2;

        Location[] locations;

        if (vert > 0) {
            locations = new Location[(searchRadius * length) + (searchRadius * length * (vert * 2))];
        } else {
            locations = new Location[searchRadius * length];
        }

        int locationNumber = 0;

        for (int r = 0; r < searchRadius; r++) {
            for (BlockFace blockFace : BlockFace.values()) {
                if (blockFace.getModY() != 0) continue;
                locations[locationNumber] = startLocation.getBlock().getRelative(blockFace, r).getLocation();
                locationNumber++;
            }
        }

        if (vert > 0) {
            for (int v = 0; v < vert; v++) {
                for (int r = 0; r < searchRadius; r++) {
                    for (BlockFace blockFace : BlockFace.values()) {
                        locations[locationNumber] = startLocation.getBlock().getRelative(blockFace,
                                r).getRelative(BlockFace.UP, v + 1).getLocation();
                        locationNumber++;
                    }

                    for (BlockFace blockFace : BlockFace.values()) {
                        locations[locationNumber] = startLocation.getBlock().getRelative(blockFace,
                                r).getRelative(BlockFace.DOWN, v + 1).getLocation();
                        locationNumber++;
                    }
                }
            }
        }

        return locations;
    }

    public static Location pickLocation(World world, double y, RegionChecker checker) {
        BlockVector3 max = checker.get().getMaximumPoint();
        BlockVector3 min = checker.get().getMinimumPoint();

        BlockVector3 v;
        do {
            v = LocationUtil.pickLocation(y, min.getX(), max.getX(), min.getZ(), max.getZ());
        } while (!checker.evaluate(v));

        return new Location(world, v.getX(), y, v.getZ());
    }

    public static Location pickLocation(World world, double y, BlockVector3 min, BlockVector3 max) {
        BlockVector3 loc = pickLocation(y, min.getX(), max.getX(), min.getZ(), max.getZ());
        return new Location(world, loc.getX(), loc.getY(), loc.getZ());
    }

    public static BlockVector3 pickLocation(double y, double minX, double maxX,
                                            double minZ, double maxZ) {

        double x;
        double z;

        if (minX > maxX) {
            x = ChanceUtil.getRangedRandom(maxX, minX);
        } else {
            x = ChanceUtil.getRangedRandom(minX, maxX);
        }

        if (minZ > maxZ) {
            z = ChanceUtil.getRangedRandom(maxZ, minZ);
        } else {
            z = ChanceUtil.getRangedRandom(minZ, maxZ);
        }

        return BlockVector3.at(x, y, z);
    }

    public static BlockVector3 pickLocation(double minX, double maxX,
                                            double minY, double maxY,
                                            double minZ, double maxZ) {

        double y;

        if (minY > maxY) {
            y = ChanceUtil.getRangedRandom(maxY, minY);
        } else {
            y = ChanceUtil.getRangedRandom(minY, maxY);
        }

        return pickLocation(y, minX, maxX, minZ, maxZ);
    }

    public static Location pickLocation(World world,
                                        double minX, double maxX,
                                        double minY, double maxY,
                                        double minZ, double maxZ) {
        BlockVector3 pos = pickLocation(minX, maxX, minY, maxY, minZ, maxZ);
        return new Location(world, pos.getX(), pos.getY(), pos.getZ());
    }

    public static Location pickLocation(World world, ProtectedRegion region) {
        final BlockVector3 min = region.getMinimumPoint();
        final BlockVector3 max = region.getMaximumPoint();

        int minX = min.getBlockX();
        int minY = min.getBlockY();
        int minZ = min.getBlockZ();
        int maxX = max.getBlockX();
        int maxY = max.getBlockY();
        int maxZ = max.getBlockZ();

        BlockVector3 v = LocationUtil.pickLocation(minX, maxX, minY, maxY, minZ, maxZ);
        return new Location(world, v.getX(), v.getY(), v.getZ());
    }

    public static boolean isLocNearby(Location startLocation, Location location, int searchRadius) {

        for (Location checkLocation : getNearbyLocations(startLocation, searchRadius)) {
            if (location.equals(checkLocation)) {
                return true;
            }
        }
        return false;
    }

    public static boolean hasBelow(Location loc, Predicate<Material> predicate) {

        Block searchBlock = loc.add(0, -1, 0).getBlock();
        if (predicate.test(searchBlock.getType())) return true;
        if (searchBlock.getType() != Material.AIR) return false;

        for (BlockFace blockFace : EnvironmentUtil.getSurroundingBlockFaces()) {
            if (predicate.test(searchBlock.getRelative(blockFace).getType())) return true;
        }
        return false;
    }

    public static boolean isChunkLoadedAt(Location loc) {
        return loc.getWorld().isChunkLoaded(loc.getBlockX() >> 4, loc.getBlockZ() >> 4);
    }
}
