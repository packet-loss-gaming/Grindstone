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
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.Dye;

import java.util.Arrays;
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
            Material.WHEAT, Material.MELON_STEM, Material.CARROT, Material.POTATO, Material.PUMPKIN_STEM
    );

    private static final Set<Material> BERRY_BUSHES = Set.of(
            Material.SWEET_BERRY_BUSH
    );

    public static boolean isBerryBush(Material type) {
        return BERRY_BUSHES.contains(type);
    }

    public static boolean isBerryBush(Block block) {
        return isBerryBush(block.getType());
    }

    public static boolean isCropBlock(Block block) {
        return isCropBlock(block.getType());
    }

    public static boolean isCropBlock(Material type) {
        return cropBlocks.contains(type) || isBerryBush(type);
    }

    private static final Set<Material> shrubBlocks = Set.of(
            Material.DEAD_BUSH, Material.GRASS, Material.TALL_GRASS, Material.ROSE_BUSH, Material.DANDELION,
            Material.RED_MUSHROOM, Material.BROWN_MUSHROOM, Material.POPPY
    );

    public static boolean isShrubBlock(Block block) {
        return isShrubBlock(block.getType());
    }

    public static boolean isShrubBlock(Material type) {
        return shrubBlocks.contains(type) || isCropBlock(type);
    }

    private static final Set<Material> valuableOres = Set.of(
            Material.GOLD_ORE, Material.LAPIS_ORE, Material.IRON_ORE, Material.DIAMOND_ORE,
            Material.REDSTONE_ORE, Material.EMERALD_ORE, Material.NETHER_QUARTZ_ORE
    );

    public static boolean isValuableOre(Block block) {
        return isValuableOre(block.getType());
    }

    public static boolean isValuableOre(Material type) {
        return valuableOres.contains(type);
    }

    private static final Set<Material> invaluableOres = Set.of(Material.COAL_ORE);

    public static boolean isOre(Block block) {
        return isOre(block.getType());
    }

    public static boolean isOre(Material type) {
        return invaluableOres.contains(type) || isValuableOre(type);
    }

    private static final Set<Material> LOGS = Set.of(
            Material.ACACIA_LOG,
            Material.BIRCH_LOG,
            Material.DARK_OAK_LOG,
            Material.JUNGLE_LOG,
            Material.SPRUCE_LOG,
            Material.OAK_LOG
    );

    public static boolean isLog(Block block) {
        return isLog(block.getType());
    }

    public static boolean isLog(Material type) {
        return LOGS.contains(type);
    }

    private static final Set<Material> SAPLINGS = Set.of(
            Material.ACACIA_SAPLING,
            Material.BIRCH_SAPLING,
            Material.DARK_OAK_SAPLING,
            Material.JUNGLE_SAPLING,
            Material.SPRUCE_SAPLING,
            Material.OAK_SAPLING
    );

    public static boolean isSapling(Block block) {
        return isSapling(block.getType());
    }

    public static boolean isSapling(Material type) {
        return SAPLINGS.contains(type);
    }

    public static ItemStack getOreDrop(Block block, boolean hasSilkTouch) {
        return getOreDrop(block.getType(), hasSilkTouch);
    }

    public static ItemStack getOreDrop(Material block, boolean hasSilkTouch) {

        if (!isOre(block)) {
            return null;
        } else if (hasSilkTouch) {
            return new ItemStack(block);
        } else {
            if (block == Material.COAL_ORE) {
                return new ItemStack(Material.COAL);
            }
            if (block == Material.LAPIS_ORE) {
                return new Dye(DyeColor.BLUE).toItemStack(ChanceUtil.getRangedRandom(4, 8));
            }
            if (block == Material.REDSTONE_ORE) {
                return new ItemStack(Material.REDSTONE, ChanceUtil.getRangedRandom(4, 5));
            }
            if (block == Material.DIAMOND_ORE) {
                return new ItemStack(Material.DIAMOND);
            }
            if (block == Material.EMERALD_ORE) {
                return new ItemStack(Material.EMERALD);
            }
            if (block == Material.NETHER_QUARTZ_ORE) {
                return new ItemStack(Material.QUARTZ);
            }

            return new ItemStack(block);
        }
    }

    private static final Set<Material> containerBlocks = Set.of(
            Material.BREWING_STAND, Material.CHEST, Material.DISPENSER, Material.DROPPER, Material.FURNACE,
            Material.JUKEBOX, Material.ENDER_CHEST, Material.TRAPPED_CHEST, Material.HOPPER,
            Material.SHULKER_BOX,

            // These are only valid in the inventory
            Material.WHITE_SHULKER_BOX, Material.ORANGE_SHULKER_BOX,
            Material.MAGENTA_SHULKER_BOX, Material.LIGHT_BLUE_SHULKER_BOX, Material.YELLOW_SHULKER_BOX,
            Material.LIME_SHULKER_BOX, Material.PINK_SHULKER_BOX, Material.GRAY_SHULKER_BOX,
            Material.LIGHT_GRAY_SHULKER_BOX, Material.CYAN_SHULKER_BOX, Material.PURPLE_SHULKER_BOX,
            Material.BLUE_SHULKER_BOX, Material.BROWN_SHULKER_BOX, Material.GREEN_SHULKER_BOX,
            Material.RED_SHULKER_BOX, Material.BLACK_SHULKER_BOX
    );

    public static boolean isContainer(Block block) {
        return isContainer(block.getType());
    }

    public static boolean isContainer(Material type) {
        return containerBlocks.contains(type);
    }

    public static boolean isChest(Material type) {
        return type == Material.CHEST || type == Material.TRAPPED_CHEST;
    }

    public static boolean isChest(Block block) {
        return isChest(block.getType());
    }

    private static final Set<Material> interactiveBlocks = Set.of(
            Material.CRAFTING_TABLE, Material.LOOM, Material.GRINDSTONE, Material.SMOKER,
            Material.ENCHANTING_TABLE, Material.BEACON, Material.ANVIL,
            Material.LEVER,

            Material.STONE_BUTTON,

            // Wooden Buttons
            Material.ACACIA_BUTTON,
            Material.BIRCH_BUTTON,
            Material.DARK_OAK_BUTTON,
            Material.JUNGLE_BUTTON,
            Material.SPRUCE_BUTTON,
            Material.OAK_BUTTON,

            // Doors
            Material.ACACIA_DOOR,
            Material.BIRCH_DOOR,
            Material.DARK_OAK_DOOR,
            Material.JUNGLE_DOOR,
            Material.SPRUCE_DOOR,
            Material.OAK_DOOR,

            Material.ACACIA_TRAPDOOR,
            Material.BIRCH_TRAPDOOR,
            Material.DARK_OAK_TRAPDOOR,
            Material.JUNGLE_TRAPDOOR,
            Material.SPRUCE_TRAPDOOR,
            Material.OAK_TRAPDOOR,

            // Fence Gates
            Material.ACACIA_FENCE_GATE,
            Material.BIRCH_FENCE_GATE,
            Material.DARK_OAK_FENCE_GATE,
            Material.JUNGLE_FENCE_GATE,
            Material.SPRUCE_FENCE_GATE,
            Material.OAK_FENCE_GATE
    );

    private static boolean isInteractiveBlock(Material type) {
        return interactiveBlocks.contains(type) || isContainer(type) || isBerryBush(type);
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
            Material.ACACIA_SIGN,
            Material.BIRCH_SIGN,
            Material.DARK_OAK_SIGN,
            Material.JUNGLE_SIGN,
            Material.SPRUCE_SIGN,
            Material.OAK_SIGN,

            Material.ACACIA_WALL_SIGN,
            Material.BIRCH_WALL_SIGN,
            Material.DARK_OAK_WALL_SIGN,
            Material.JUNGLE_WALL_SIGN,
            Material.SPRUCE_WALL_SIGN,
            Material.OAK_WALL_SIGN
    );

    public static boolean isSign(Block block) {
        return isSign(block.getType());
    }

    public static boolean isSign(Material type) {

        return signBlocks.contains(type);
    }

    private static final Set<Material> WOOL = Set.of(
            Material.WHITE_WOOL, Material.ORANGE_WOOL, Material.MAGENTA_WOOL,
            Material.LIGHT_BLUE_WOOL, Material.YELLOW_WOOL, Material.LIME_WOOL,
            Material.PINK_WOOL, Material.GRAY_WOOL, Material.LIGHT_GRAY_WOOL,
            Material.CYAN_WOOL, Material.PURPLE_WOOL, Material.BLUE_WOOL,
            Material.BROWN_WOOL, Material.GREEN_WOOL,
            Material.RED_WOOL, Material.BLACK_WOOL
    );

    public static boolean isWool(Material type) {
        return WOOL.contains(type);
    }

    public static boolean isWool(Block block) {
        return isWool(block.getType());
    }

    private static final Set<Material> CONCRETE = Set.of(
            Material.WHITE_CONCRETE, Material.ORANGE_CONCRETE, Material.MAGENTA_CONCRETE,
            Material.LIGHT_BLUE_CONCRETE, Material.YELLOW_CONCRETE, Material.LIME_CONCRETE,
            Material.PINK_CONCRETE, Material.GRAY_CONCRETE, Material.LIGHT_GRAY_CONCRETE,
            Material.CYAN_CONCRETE, Material.PURPLE_CONCRETE, Material.BLUE_CONCRETE,
            Material.BROWN_CONCRETE, Material.GREEN_CONCRETE,
            Material.RED_CONCRETE, Material.BLACK_CONCRETE
    );

    public static boolean isConcrete(Material type) {
        return CONCRETE.contains(type);
    }

    public static boolean isConcrete(Block block) {
        return isConcrete(block.getType());
    }

    private static final Set<Material> STAINED_GLASS_BLOCKS = Set.of(
            Material.WHITE_STAINED_GLASS, Material.ORANGE_STAINED_GLASS, Material.MAGENTA_STAINED_GLASS,
            Material.LIGHT_BLUE_STAINED_GLASS, Material.YELLOW_STAINED_GLASS, Material.LIME_STAINED_GLASS,
            Material.PINK_STAINED_GLASS, Material.GRAY_STAINED_GLASS, Material.LIGHT_GRAY_STAINED_GLASS,
            Material.CYAN_STAINED_GLASS, Material.PURPLE_STAINED_GLASS, Material.BLUE_STAINED_GLASS,
            Material.BROWN_STAINED_GLASS, Material.GREEN_STAINED_GLASS,
            Material.RED_STAINED_GLASS, Material.BLACK_STAINED_GLASS
    );

    public static boolean isStainedGlassBlock(Material type) {
        return STAINED_GLASS_BLOCKS.contains(type);
    }

    public static boolean isStainedGlassBlock(Block block) {
        return isStainedGlassBlock(block.getType());
    }

    private static final Set<Material> STAINED_GLASS_PANES = Set.of(
            Material.WHITE_STAINED_GLASS_PANE, Material.ORANGE_STAINED_GLASS_PANE,
            Material.MAGENTA_STAINED_GLASS_PANE, Material.LIGHT_BLUE_STAINED_GLASS_PANE,
            Material.YELLOW_STAINED_GLASS_PANE, Material.LIME_STAINED_GLASS_PANE,
            Material.PINK_STAINED_GLASS_PANE, Material.GRAY_STAINED_GLASS_PANE,
            Material.LIGHT_GRAY_STAINED_GLASS_PANE, Material.CYAN_STAINED_GLASS_PANE,
            Material.PURPLE_STAINED_GLASS_PANE, Material.BLUE_STAINED_GLASS_PANE,
            Material.BROWN_STAINED_GLASS_PANE, Material.GREEN_STAINED_GLASS_PANE,
            Material.RED_STAINED_GLASS_PANE, Material.BLACK_STAINED_GLASS_PANE
    );

    public static boolean isStainedGlassPane(Material type) {
        return STAINED_GLASS_PANES.contains(type);
    }

    public static boolean isStainedGlassPane(Block block) {
        return isStainedGlassPane(block.getType());
    }

    public static boolean isStainedGlass(Material type) {
        return isStainedGlassPane(type) || isStainedGlassBlock(type);
    }

    public static boolean isStainedGlass(Block block) {
        return isStainedGlass(block.getType());
    }

    private static final Set<Material> BEDS = Set.of(
            Material.WHITE_BED, Material.ORANGE_BED,
            Material.MAGENTA_BED, Material.LIGHT_BLUE_BED,
            Material.YELLOW_BED, Material.LIME_BED,
            Material.PINK_BED, Material.GRAY_BED,
            Material.LIGHT_GRAY_BED, Material.CYAN_BED,
            Material.PURPLE_BED, Material.BLUE_BED,
            Material.BROWN_BED, Material.GREEN_BED,
            Material.RED_BED, Material.BLACK_BED
    );

    public static boolean isBed(Material type) {
        return BEDS.contains(type);
    }

    public static boolean isBed(Block block) {
        return isBed(block.getType());
    }

    public static boolean isWater(Block block) {
        return isWater(block.getType());
    }

    public static boolean isWater(Material type) {
        return type == Material.WATER;
    }

    public static boolean isLava(Block block) {
        return isLava(block.getType());
    }

    public static boolean isLava(Material type) {
        return type == Material.LAVA;
    }

    public static boolean isLiquid(Material type) {

        return isWater(type) || isLava(type);
    }

    public static boolean isDangerous(Material type) {
        return isLava(type);
    }

    public static boolean isDangerous(Block block) {
        return isDangerous(block.getType());
    }

    private static final Set<Material> TREE_GROWING_BLOCKS = Set.of(
            Material.DIRT, Material.GRASS_BLOCK
    );

    public static boolean canTreeGrownOn(Material material) {
        return TREE_GROWING_BLOCKS.contains(material);
    }

    public static boolean canTreeGrownOn(Block block) {
        return canTreeGrownOn(block.getType());
    }

    private static final Set<Material> NATURAL_TERRAIN_BLOCKS = Set.of(
            Material.DIRT, Material.PODZOL, Material.GRASS_BLOCK,
            Material.STONE
    );

    public static boolean isNaturalTerrainBlock(Material material) {
        return NATURAL_TERRAIN_BLOCKS.contains(material);
    }

    public static boolean isNaturalTerrainBlock(Block block) {
        return isNaturalTerrainBlock(block.getType());
    }

    private static final Set<Biome> FROZEN_BIOMES = Set.of(
            Biome.DEEP_FROZEN_OCEAN, Biome.FROZEN_OCEAN, Biome.FROZEN_RIVER,
            Biome.SNOWY_BEACH, Biome.SNOWY_MOUNTAINS, Biome.SNOWY_TAIGA,
            Biome.SNOWY_TAIGA_HILLS, Biome.SNOWY_TAIGA_MOUNTAINS, Biome.SNOWY_TUNDRA,
            Biome.ICE_SPIKES
    );

    public static boolean isFrozenBiome(Biome biome) {

        return FROZEN_BIOMES.contains(biome);
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

    private static final BlockFace[] NEARBY_BLOCK_FACES = new BlockFace[]{
            BlockFace.NORTH, BlockFace.EAST,
            BlockFace.SOUTH, BlockFace.WEST,
            BlockFace.NORTH_EAST, BlockFace.NORTH_WEST,
            BlockFace.SOUTH_EAST, BlockFace.SOUTH_WEST,
            BlockFace.SELF
    };

    public static BlockFace[] getNearbyBlockFaces() {
        return NEARBY_BLOCK_FACES;
    }

    private static final BlockFace[] CARDINAL_BLOCK_FACES = new BlockFace[] {
            BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST
    };

    public static BlockFace[] getCardinalBlockFaces() {
        return CARDINAL_BLOCK_FACES;
    }

    private static final BlockFace[] SURROUNDING_BLOCK_FACES = new BlockFace[]{
            BlockFace.NORTH, BlockFace.EAST,
            BlockFace.SOUTH, BlockFace.WEST,
            BlockFace.NORTH_EAST, BlockFace.NORTH_WEST,
            BlockFace.SOUTH_EAST, BlockFace.SOUTH_WEST
    };

    public static BlockFace[] getSurroundingBlockFaces() {
        return SURROUNDING_BLOCK_FACES;
    }

    private static SpawnReason[] NON_NATURAL_SPAWN_REASONS = new SpawnReason[] {
            SpawnReason.CUSTOM, SpawnReason.SPAWNER, SpawnReason.SPAWNER_EGG,
            SpawnReason.SLIME_SPLIT, SpawnReason.ENDER_PEARL, SpawnReason.SILVERFISH_BLOCK,
            SpawnReason.BUILD_WITHER
    };

    public static boolean isNonNaturalSpawnReason(SpawnReason reason) {
        return Arrays.asList(NON_NATURAL_SPAWN_REASONS).contains(reason);
    }
}