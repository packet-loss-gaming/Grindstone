package com.skelril.aurora.util;

import com.sk89q.worldedit.BlockVector;
import com.sk89q.worldedit.blocks.BlockID;
import com.sk89q.worldedit.blocks.BlockType;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

public class LocationUtil {

    private static final BlockFace[] surroundingBlockFaces = new BlockFace[]{
            BlockFace.NORTH, BlockFace.EAST,
            BlockFace.SOUTH, BlockFace.WEST, BlockFace.NORTH_EAST, BlockFace.NORTH_WEST, BlockFace.SOUTH_EAST,
            BlockFace.SOUTH_WEST
    };

    @Deprecated
    private static Location matchLocationFromId(int id) {

        int trueId = id - 1;
        Location[] TeleportLocations = new Location[5];
        TeleportLocations[0] = new Location(Bukkit.getWorld("Destrio"), -594, 10, 1180);
        TeleportLocations[1] = Bukkit.getWorld("Destrio").getSpawnLocation();
        TeleportLocations[2] = new Location(Bukkit.getWorld("Destrio"), 36, 79, 1125);
        TeleportLocations[3] = new Location(Bukkit.getWorld("Destrio"), -285, 140, -664);
        TeleportLocations[4] = new Location(Bukkit.getWorld("Destrio"), -59, 98, -1698);
        for (int i = 0; i < TeleportLocations.length; i++) {
            if (i == trueId) return TeleportLocations[i];
        }
        return null;
    }

    @Deprecated
    private static int getLocationId(String name) {

        String[] LocationNames = new String[5];
        LocationNames[0] = "THE BANK";
        LocationNames[1] = "FLINT";
        LocationNames[2] = "FORT";
        LocationNames[3] = "SKY CITY";
        LocationNames[4] = "MUSHROOM ISLE";


        for (int i = 0; i < LocationNames.length; i++) {
            if (LocationNames[i].equalsIgnoreCase(name)) {
                return i + 1;
            }
        }
        return 0;
    }

    @Deprecated
    public static Location matchLocationFromText(String name) {

        return matchLocationFromId(getLocationId(name));
    }

    public static Location grandBank(World world) {

        if (!world.getName().equals("City")) return null;
        return new Location(world, 592, 83, 1176.5);
    }

    public static boolean toGround(Player player) {

        return player.teleport(findFreePosition(player.getLocation()));
    }

    public static Location findFreePosition(Location pos) {

        World world = pos.getWorld();

        // Let's try going down
        Block block = pos.getBlock().getRelative(0, 1, 0);
        if (!block.getChunk().isLoaded()) {
            block.getChunk().load();
        }
        int free = 0;

        // Look for ground
        while (block.getY() > 1 && (BlockType.canPassThrough(block.getTypeId(), block.getData()) || block.getTypeId() == BlockID.BED)) {
            free++;
            block = block.getRelative(0, -1, 0);
        }

        if (block.getY() == 0) return null; // No ground below!

        if (free >= 2) {
            if (block.getTypeId() == BlockID.LAVA || block.getTypeId() == BlockID.STATIONARY_LAVA) {
                return null; // Not safe
            }

            Block tb = block.getRelative(0, 1, 0);
            Location l = tb.getLocation();
            l.add(new Vector(.5, BlockType.centralTopLimit(tb.getTypeId(), tb.getData()), .5));
            l.setPitch(pos.getPitch());
            l.setYaw(pos.getYaw());
            return l;
        }

        // Let's try going up
        block = pos.getBlock().getRelative(0, -1, 0);
        free = 0;
        boolean foundGround = false;

        while (block.getY() + 1 < world.getMaxHeight()) {
            if (BlockType.canPassThrough(block.getTypeId(), block.getData()) || block.getTypeId() == BlockID.BED) {
                free++;
            } else {
                free = 0;
                foundGround = block.getTypeId() != BlockID.LAVA && block.getTypeId() != BlockID.STATIONARY_LAVA;
            }

            if (foundGround && free == 2) {
                Block tb = block.getRelative(0, -1, 0);
                Location l = tb.getLocation();
                l.add(new Vector(.5, BlockType.centralTopLimit(tb.getTypeId(), tb.getData()), .5));
                l.setPitch(pos.getPitch());
                l.setYaw(pos.getYaw());
                return l;
            }

            block = block.getRelative(0, 1, 0);
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

        BlockFace dir;
        do {
            dir = BlockFace.values()[(ChanceUtil.getRandom(BlockFace.values().length) - 1)];
        } while (dir == null);

        if (airOnly) {
            return findFreePosition(searchFromLocation.getRelative(dir, trueRadius).getLocation());
        } else {
            return searchFromLocation.getRelative(dir, trueRadius).getLocation();
        }
    }

    public static boolean containsPlayer(World world, ProtectedRegion region) {

        for (Player player : world.getPlayers()) {
            Location loc = player.getLocation();
            if (region.contains(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ())) {
                return true;
            }
        }
        return false;
    }

    public static boolean isInRegion(World world, ProtectedRegion region, Entity entity) {

        return isInRegion(world, region, entity.getLocation());
    }

    public static boolean isInRegion(World world, ProtectedRegion region, Location loc) {

        return world.equals(loc.getWorld()) && region.contains(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());

    }

    public static List<Player> getPlayersStandingOnRegion(World world, ProtectedRegion region) {

        List<Player> playerList = new ArrayList<>();
        for (Player player : world.getPlayers()) {
            Block block = player.getLocation().getBlock();
            if (region.contains(block.getX(), block.getY() - 1, block.getZ())
                    || region.contains(block.getX(), block.getY() - 2, block.getZ())) {
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

        Location[] locations;

        if (vert > 0) {
            locations = new Location[(searchRadius * BlockFace.values().length) + (searchRadius * BlockFace.values()
                    .length * (vert * 2))];
        } else {
            locations = new Location[searchRadius * BlockFace.values().length];
        }

        int locationNumber = 0;

        for (int r = 0; r < searchRadius; r++) {
            for (BlockFace blockFace : BlockFace.values()) {
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

    public static com.sk89q.worldedit.Vector pickLocation(BlockVector min, BlockVector max) {

        return pickLocation(min.getX(), max.getX(), min.getZ(), max.getZ());
    }

    public static com.sk89q.worldedit.Vector pickLocation(double minX, double maxX,
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

        return new com.sk89q.worldedit.Vector(x, 0, z);
    }

    public static com.sk89q.worldedit.Vector pickLocation(double minX, double maxX,
                                                          double minY, double maxY,
                                                          double minZ, double maxZ) {

        double y;

        if (minY > maxY) {
            y = ChanceUtil.getRangedRandom(maxY, minY);
        } else {
            y = ChanceUtil.getRangedRandom(minY, maxY);
        }

        return pickLocation(minX, maxX, minZ, maxZ).add(0, y, 0);
    }

    public static boolean isLocNearby(Location startLocation, Location location, int searchRadius) {

        for (Location checkLocation : getNearbyLocations(startLocation, searchRadius)) {
            if (location.equals(checkLocation)) {
                return true;
            }
        }
        return false;
    }

    public static Location parseLocation(String string) {

        try {
            String[] locationString = string.split(",");
            return new Location(Bukkit.getWorld(locationString[3]),
                    Integer.parseInt(locationString[0]),
                    Integer.parseInt(locationString[1]),
                    Integer.parseInt(locationString[2]));

        } catch (Exception e) {
            return null;
        }
    }

    public static boolean getBelowID(Location loc, int id) {

        Block searchBlock = loc.add(0, -1, 0).getBlock();
        if (searchBlock.getTypeId() == id) return true;
        if (searchBlock.getTypeId() != BlockID.AIR) return false;

        for (BlockFace blockFace : surroundingBlockFaces) {
            if (searchBlock.getRelative(blockFace).getTypeId() == id) return true;
        }
        return false;
    }
}
