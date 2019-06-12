/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.util;

import org.bukkit.*;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Monster;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.Dye;

import java.util.Set;

public class EnvironmentUtil {

    public static boolean isNightTime(long time) {

        return !isDayTime(time);
    }

    public static boolean isDayTime(long time) {

        if (time < 0) {
            time += 24000;
        }

        return time >= 0L && time <= 13000L;
    }

    public static boolean isServerTimeOdd(long time) {

        long t = time % 2;
        if (t < 0) t += 2;
        return (t == 1);
    }

    public static boolean isMidnight(long time) {

        return time == ((0 - 8 + 24) * 1000);
    }

    public static boolean hasThunderstorm(World world) {
        return world.hasStorm() && world.isThundering();
    }

    private static final Set<Material> cropBlocks = Set.of(
            Material.CROPS, Material.MELON_STEM, Material.CARROT, Material.POTATO, Material.PUMPKIN_STEM
    );

    public static boolean isCropBlock(Block block) {

        return isCropBlock(block.getType());
    }

    public static boolean isCropBlock(Material block) {

        return cropBlocks.contains(block);
    }

    private static final Set<Material> shrubBlocks = Set.of(
            Material.DEAD_BUSH, Material.LONG_GRASS, Material.RED_ROSE, Material.YELLOW_FLOWER,
            Material.DOUBLE_PLANT, Material.RED_MUSHROOM, Material.BROWN_MUSHROOM
    );

    public static boolean isShrubBlock(Block block) {

        return isShrubBlock(block.getType());
    }

    public static boolean isShrubBlock(Material material) {

        return shrubBlocks.contains(material) || isCropBlock(material);
    }


    private static final Set<Material> valuableOres = Set.of(
            Material.GOLD_ORE, Material.LAPIS_ORE, Material.IRON_ORE, Material.DIAMOND_ORE,
            Material.REDSTONE_ORE, Material.GLOWING_REDSTONE_ORE, Material.EMERALD_ORE, Material.QUARTZ_ORE
    );

    public static boolean isValuableOre(Block block) {

        return isValuableOre(block.getType());
    }

    public static boolean isValuableOre(Material material) {

        return valuableOres.contains(material);
    }

    private static final Set<Material> invaluableOres = Set.of(Material.COAL_ORE);

    public static boolean isOre(Block block) {

        return isOre(block.getType());
    }

    public static boolean isOre(Material material) {

        return invaluableOres.contains(material) || isValuableOre(material);
    }

    public static ItemStack getOreDrop(Block block, boolean hasSilkTouch) {

        return getOreDrop(block.getType(), hasSilkTouch);
    }

    public static ItemStack getOreDrop(Material block, boolean hasSilkTouch) {

        if (!isOre(block)) {
            return null;
        } else if (hasSilkTouch) {
            if (block == Material.GLOWING_REDSTONE_ORE) {
                return new ItemStack(Material.REDSTONE_ORE);
            }

            return new ItemStack(block);
        } else {
            if (block == Material.COAL_ORE) {
                return new ItemStack(Material.COAL);
            }
            if (block == Material.LAPIS_ORE) {
                ItemStack lapis = new ItemStack(Material.INK_SACK, ChanceUtil.getRangedRandom(4, 8));
                lapis.setData(new Dye(DyeColor.BLUE));
                return lapis;
            }
            if (block == Material.REDSTONE_ORE || block == Material.GLOWING_REDSTONE_ORE) {
                return new ItemStack(Material.REDSTONE, ChanceUtil.getRangedRandom(4, 5));
            }
            if (block == Material.DIAMOND_ORE) {
                return new ItemStack(Material.DIAMOND);
            }
            if (block == Material.EMERALD_ORE) {
                return new ItemStack(Material.EMERALD);
            }
            if (block == Material.QUARTZ_ORE) {
                return new ItemStack(Material.QUARTZ);
            }

            return new ItemStack(block);
        }
    }

    private static final Set<Material> containerBlocks = Set.of(
            Material.BREWING_STAND, Material.CHEST, Material.DISPENSER, Material.DROPPER, Material.FURNACE,
            Material.BURNING_FURNACE, Material.JUKEBOX, Material.ENDER_CHEST, Material.TRAPPED_CHEST, Material.HOPPER
    );

    public static boolean isContainer(Block block) {

        return isContainer(block.getType());
    }

    public static boolean isContainer(Material material) {

        return containerBlocks.contains(material);
    }

    public static boolean isChest(Material material) {
        return material == Material.CHEST || material == Material.TRAPPED_CHEST;
    }

    public static boolean isChest(Block block) {
        return isChest(block.getType());
    }

    private static final Set<Material> interactiveBlocks = Set.of(
            Material.WORKBENCH, Material.ENCHANTMENT_TABLE, Material.BEACON, Material.ANVIL,
            Material.LEVER, Material.STONE_BUTTON, Material.WOOD_BUTTON, Material.WOODEN_DOOR,
            Material.FENCE_GATE, Material.TRAP_DOOR
    );

    private static boolean isInteractiveBlock(Material material) {

        return interactiveBlocks.contains(material) || isContainer(material);
    }

    public static boolean isInteractiveBlock(Block block) {
        if (isInteractiveBlock(block.getType()))
            return true;

        if (block.getState() instanceof Sign) {
            Sign signState = (Sign) block.getState();
            for (String line : signState.getLines()) {
                if (line.matches("\\[.*]")) {
                    return true;
                }
            }
        }

        return false;
    }

    public static boolean isMaybeInteractiveBlock(Block clicked, BlockFace clickedFace) {
        if (EnvironmentUtil.isInteractiveBlock(clicked)) {
            return true;
        }

        if (EnvironmentUtil.isInteractiveBlock(clicked.getRelative(clickedFace))) {
            return true;
        }

        return false;
    }

    private static final Set<Material> signBlocks = Set.of(
        Material.WALL_SIGN, Material.SIGN_POST
    );

    public static boolean isSign(Block block) {

        return isSign(block.getType());
    }

    public static boolean isSign(Material material) {

        return signBlocks.contains(material);
    }

    private static final Set<Material> waterBlocks = Set.of(
            Material.STATIONARY_WATER, Material.WATER
    );

    public static boolean isWater(Block block) {

        return isWater(block.getType());
    }

    public static boolean isWater(Material material) {

        return waterBlocks.contains(material);
    }

    private static final Set<Material> lavaBlocks = Set.of(
            Material.STATIONARY_LAVA, Material.LAVA
    );

    public static boolean isLava(Block block) {

        return isLava(block.getType());
    }

    public static boolean isLava(Material material) {

        return lavaBlocks.contains(material);
    }

    public static boolean isLiquid(Material material) {

        return isWater(material) || isLava(material);
    }

    private static final Set<Biome> frozenBiomes = Set.of(
            Biome.FROZEN_OCEAN, Biome.FROZEN_RIVER, Biome.ICE_MOUNTAINS, Biome.ICE_FLATS, Biome.MUTATED_ICE_FLATS
    );

    public static boolean isFrozenBiome(Biome biome) {

        return frozenBiomes.contains(biome);
    }

    public static void generateRadialEffect(Location location, Effect effect) {

        for (int i = 0; i < 20; i++) {
            location.getWorld().playEffect(location, effect, ChanceUtil.getRandom(9) - 1);
        }
    }

    public static void generateRadialEffect(Location[] locations, Effect effect) {

        for (Location loc : locations) generateRadialEffect(loc, effect);
    }

    public static boolean isHostileEntity(Entity e) {

        return e instanceof Monster;
    }

    private static final BlockFace[] nearby = new BlockFace[]{
            BlockFace.NORTH, BlockFace.NORTH_EAST, BlockFace.EAST, BlockFace.NORTH_WEST,
            BlockFace.SOUTH, BlockFace.SOUTH_EAST, BlockFace.WEST, BlockFace.SOUTH_WEST,
            BlockFace.SELF
    };

    public static BlockFace[] getNearbyBlockFaces() {

        return nearby;
    }
}