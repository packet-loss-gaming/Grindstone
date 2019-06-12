/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.util;

import com.sk89q.worldedit.blocks.BlockID;
import com.sk89q.worldedit.blocks.ItemID;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;

import java.util.HashSet;
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

    private static final Set<Integer> cropBlocks = new HashSet<>();

    static {
        cropBlocks.add(BlockID.CROPS);
        cropBlocks.add(BlockID.MELON_STEM);
        cropBlocks.add(BlockID.CARROTS);
        cropBlocks.add(BlockID.POTATOES);
        cropBlocks.add(BlockID.PUMPKIN_STEM);
    }

    public static boolean isCropBlock(Block block) {

        return isCropBlock(block.getTypeId());
    }

    public static boolean isCropBlock(int block) {

        return cropBlocks.contains(block);
    }

    private static final Set<Integer> shrubBlocks = new HashSet<>();

    static {
        shrubBlocks.add(BlockID.DEAD_BUSH);
        shrubBlocks.add(BlockID.LONG_GRASS);
        shrubBlocks.add(BlockID.RED_FLOWER);
        shrubBlocks.add(BlockID.YELLOW_FLOWER);
        shrubBlocks.add(BlockID.DOUBLE_PLANT);
        shrubBlocks.add(BlockID.RED_MUSHROOM);
        shrubBlocks.add(BlockID.BROWN_MUSHROOM);
    }

    public static boolean isShrubBlock(Block block) {

        return isShrubBlock(block.getTypeId());
    }

    public static boolean isShrubBlock(int block) {

        return shrubBlocks.contains(block) || isCropBlock(block);
    }

    private static final Set<Integer> invaluableOres = new HashSet<>();

    static {
        invaluableOres.add(BlockID.COAL_ORE);
    }

    public static boolean isOre(Block block) {

        return isOre(block.getTypeId());
    }

    public static boolean isOre(int block) {

        return invaluableOres.contains(block) || isValuableOre(block);
    }

    public static boolean isValuableBlockOrOre(Block block) {

        return isValuableBlockOrOre(block.getTypeId());
    }

    public static boolean isValuableBlockOrOre(int block) {

        return isValuableBlock(block) || isValuableOre(block);
    }

    public static boolean isValuableBlock(Block block) {

        return isValuableBlock(block.getTypeId());
    }

    private static final Set<Integer> valuableBlocks = new HashSet<>();

    static {
        valuableBlocks.add(BlockID.GOLD_BLOCK);
        valuableBlocks.add(BlockID.LAPIS_LAZULI_BLOCK);
        valuableBlocks.add(BlockID.IRON_BLOCK);
        valuableBlocks.add(BlockID.DIAMOND_BLOCK);
        valuableBlocks.add(BlockID.EMERALD_BLOCK);
        valuableBlocks.add(BlockID.BEACON);
        valuableBlocks.add(BlockID.COMMAND_BLOCK);
        valuableBlocks.add(BlockID.COAL_BLOCK);
    }

    public static boolean isValuableBlock(int block) {

        return valuableBlocks.contains(block);
    }

    private static final Set<Integer> valuableOres = new HashSet<>();

    static {
        valuableOres.add(BlockID.GOLD_ORE);
        valuableOres.add(BlockID.LAPIS_LAZULI_ORE);
        valuableOres.add(BlockID.IRON_ORE);
        valuableOres.add(BlockID.DIAMOND_ORE);
        valuableOres.add(BlockID.REDSTONE_ORE);
        valuableOres.add(BlockID.GLOWING_REDSTONE_ORE);
        valuableOres.add(BlockID.EMERALD_ORE);
        valuableOres.add(BlockID.QUARTZ_ORE);
    }

    public static boolean isValuableOre(Block block) {

        return isValuableOre(block.getTypeId());
    }

    public static boolean isValuableOre(int block) {

        return valuableOres.contains(block);
    }

    public static ItemStack getOreDrop(Block block, boolean hasSilkTouch) {

        return getOreDrop(block.getTypeId(), hasSilkTouch);
    }

    public static ItemStack getOreDrop(int block, boolean hasSilkTouch) {

        if (!isOre(block)) {
            return null;
        } else if (hasSilkTouch) {
            switch (block) {
                case BlockID.GLOWING_REDSTONE_ORE:
                    return new ItemStack(BlockID.REDSTONE_ORE);
                default:
                    return new ItemStack(block);
            }
        } else {
            switch (block) {
                case BlockID.IRON_ORE:
                    return new ItemStack(BlockID.IRON_ORE);
                case BlockID.COAL_ORE:
                    return new ItemStack(ItemID.COAL);
                case BlockID.GOLD_ORE:
                    return new ItemStack(BlockID.GOLD_ORE);
                case BlockID.LAPIS_LAZULI_ORE:
                    return new ItemStack(ItemID.INK_SACK, ChanceUtil.getRangedRandom(4, 8), (short) 4);
                case BlockID.REDSTONE_ORE:
                case BlockID.GLOWING_REDSTONE_ORE:
                    return new ItemStack(ItemID.REDSTONE_DUST, ChanceUtil.getRangedRandom(4, 5));
                case BlockID.DIAMOND_ORE:
                    return new ItemStack(ItemID.DIAMOND);
                case BlockID.EMERALD_ORE:
                    return new ItemStack(ItemID.EMERALD);
                case BlockID.QUARTZ_ORE:
                    return new ItemStack(ItemID.NETHER_QUARTZ);
                default:
                    return null;
            }
        }
    }

    private static final Set<Integer> containerBlocks = new HashSet<>();

    static {
        containerBlocks.add(BlockID.BREWING_STAND);
        containerBlocks.add(BlockID.CHEST);
        containerBlocks.add(BlockID.DISPENSER);
        containerBlocks.add(BlockID.DROPPER);
        containerBlocks.add(BlockID.FURNACE);
        containerBlocks.add(BlockID.BURNING_FURNACE);
        containerBlocks.add(BlockID.JUKEBOX);
        containerBlocks.add(BlockID.ENDER_CHEST);
        containerBlocks.add(BlockID.TRAPPED_CHEST);
        containerBlocks.add(BlockID.HOPPER);
    }

    public static boolean isContainer(Block block) {

        return isContainer(block.getTypeId());
    }

    public static boolean isContainer(int block) {

        return containerBlocks.contains(block);
    }

    public static boolean isChest(Material material) {
        return material == Material.CHEST || material == Material.TRAPPED_CHEST;
    }

    public static boolean isChest(Block block) {
        return isChest(block.getType());
    }

    private static final Set<Integer> interactiveBlocks = new HashSet<>();

    static {
        interactiveBlocks.add(BlockID.WORKBENCH);
        interactiveBlocks.add(BlockID.ENCHANTMENT_TABLE);
        interactiveBlocks.add(BlockID.BEACON);
        interactiveBlocks.add(BlockID.ANVIL);
        interactiveBlocks.add(BlockID.LEVER);
        interactiveBlocks.add(BlockID.STONE_BUTTON);
        interactiveBlocks.add(BlockID.WOODEN_BUTTON);
        interactiveBlocks.add(BlockID.WOODEN_DOOR);
        interactiveBlocks.add(BlockID.FENCE_GATE);
        interactiveBlocks.add(BlockID.TRAP_DOOR);
    }

    private static boolean isInteractiveBlock(int block) {

        return interactiveBlocks.contains(block) || isContainer(block);
    }

    public static boolean isInteractiveBlock(Block block) {
        if (isInteractiveBlock(block.getTypeId()))
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

    private static final Set<Integer> signBlocks = new HashSet<>();

    static {
        signBlocks.add(BlockID.SIGN_POST);
        signBlocks.add(BlockID.WALL_SIGN);
    }

    public static boolean isSign(Block block) {

        return isSign(block.getTypeId());
    }

    public static boolean isSign(int block) {

        return signBlocks.contains(block);
    }

    private static final Set<Integer> waterBlocks = new HashSet<>();

    static {
        waterBlocks.add(BlockID.WATER);
        waterBlocks.add(BlockID.STATIONARY_WATER);
    }

    public static boolean isWater(Block block) {

        return isWater(block.getTypeId());
    }

    public static boolean isWater(int block) {

        return waterBlocks.contains(block);
    }

    private static final Set<Integer> lavaBlocks = new HashSet<>();

    static {
        lavaBlocks.add(BlockID.LAVA);
        lavaBlocks.add(BlockID.STATIONARY_LAVA);
    }

    public static boolean isLava(Block block) {

        return isLava(block.getTypeId());
    }

    public static boolean isLava(int block) {

        return lavaBlocks.contains(block);
    }

    public static boolean isLiquid(int block) {

        return isWater(block) || isLava(block);
    }

    private static final Set<Biome> frozenBiomes = new HashSet<>();

    static {
        frozenBiomes.add(Biome.FROZEN_OCEAN);
        frozenBiomes.add(Biome.FROZEN_RIVER);
        frozenBiomes.add(Biome.ICE_MOUNTAINS);
        frozenBiomes.add(Biome.ICE_FLATS);
        frozenBiomes.add(Biome.MUTATED_ICE_FLATS);
    }

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

    private static Set<EntityType> hostileEntities = new HashSet<>();

    static {
        hostileEntities.add(EntityType.ENDERMAN);
        hostileEntities.add(EntityType.PIG_ZOMBIE);
        hostileEntities.add(EntityType.ZOMBIE);
        hostileEntities.add(EntityType.SKELETON);
        hostileEntities.add(EntityType.CREEPER);
        hostileEntities.add(EntityType.SILVERFISH);
        hostileEntities.add(EntityType.SPIDER);
        hostileEntities.add(EntityType.CAVE_SPIDER);
        hostileEntities.add(EntityType.SLIME);
        hostileEntities.add(EntityType.MAGMA_CUBE);
        hostileEntities.add(EntityType.BLAZE);
        hostileEntities.add(EntityType.WITCH);
    }

    public static boolean isHostileEntity(Entity e) {

        return e != null && e.isValid() && e.getType() != null && hostileEntities.contains(e.getType());
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