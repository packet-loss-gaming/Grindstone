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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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

    private static final Set<Material> ORES;

    static {
        List<Material> newOreBlocks = new ArrayList<>();

        for (Material material : Material.values()) {
            if (material.name().endsWith("_ORE")) {
                newOreBlocks.add(material);
            }
        }

        ORES = Set.copyOf(newOreBlocks);
    }

    public static boolean isOre(Block block) {
        return isOre(block.getType());
    }

    public static boolean isOre(Material type) {
        return ORES.contains(type);
    }

    private static final Set<Material> LOGS;

    static {
        List<Material> newSaplingsBlocks = new ArrayList<>();

        for (Material material : Material.values()) {
            if (material.name().endsWith("_LOG")) {
                newSaplingsBlocks.add(material);
            }
        }

        LOGS = Set.copyOf(newSaplingsBlocks);
    }

    public static boolean isLog(Block block) {
        return isLog(block.getType());
    }

    public static boolean isLog(Material type) {
        return LOGS.contains(type);
    }

    private static final Set<Material> SAPLINGS;

    static {
        List<Material> newSaplingsBlocks = new ArrayList<>();

        for (Material material : Material.values()) {
            if (material.name().endsWith("_SAPLING")) {
                newSaplingsBlocks.add(material);
            }
        }

        SAPLINGS = Set.copyOf(newSaplingsBlocks);
    }

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

    private static final Set<Material> CONTAINER_BLOCKS;

    static {
        List<Material> newContainerBlocks = new ArrayList<>(List.of(
                Material.BREWING_STAND, Material.CHEST, Material.DISPENSER, Material.DROPPER, Material.FURNACE,
                Material.JUKEBOX, Material.ENDER_CHEST, Material.TRAPPED_CHEST, Material.HOPPER,
                Material.SHULKER_BOX
        ));

        for (Material material : Material.values()) {
            if (material.name().endsWith("_SHULKER_BOX")) {
                newContainerBlocks.add(material);
            }
        }

        CONTAINER_BLOCKS = Set.copyOf(newContainerBlocks);
    }

    public static boolean isContainer(Block block) {
        return isContainer(block.getType());
    }

    public static boolean isContainer(Material type) {
        return CONTAINER_BLOCKS.contains(type);
    }

    public static boolean isChest(Material type) {
        return type == Material.CHEST || type == Material.TRAPPED_CHEST;
    }

    public static boolean isChest(Block block) {
        return isChest(block.getType());
    }

    private static final Set<Material> CLOSEABLE_BLOCKS;

    static {
        List<Material> newInteractiveBlocks = new ArrayList<>();

        for (Material material : Material.values()) {
            String name = material.name();
            if (name.endsWith("_DOOR")) {
                if (material == Material.IRON_DOOR) {
                    continue;
                }

                newInteractiveBlocks.add(material);
            } else if (name.endsWith("_TRAPDOOR")) {
                newInteractiveBlocks.add(material);
            } else if (name.endsWith("_FENCE_GATE")) {
                newInteractiveBlocks.add(material);
            }
        }

        CLOSEABLE_BLOCKS = Set.copyOf(newInteractiveBlocks);
    }

    public static boolean isClosable(Material type) {
        return CLOSEABLE_BLOCKS.contains(type);
    }

    public static boolean isClosable(Block block) {
        return isClosable(block.getType());
    }

    private static final Set<Material> INTERACTIVE_BLOCKS;

    static {
        List<Material> newInteractiveBlocks = new ArrayList<>(List.of(
                Material.CRAFTING_TABLE, Material.LOOM, Material.GRINDSTONE, Material.SMOKER,
                Material.ENCHANTING_TABLE, Material.BEACON, Material.ANVIL,
                Material.LEVER
        ));

        newInteractiveBlocks.addAll(CONTAINER_BLOCKS);
        newInteractiveBlocks.addAll(CLOSEABLE_BLOCKS);
        newInteractiveBlocks.addAll(BERRY_BUSHES);

        for (Material material : Material.values()) {
            String name = material.name();
            if (name.endsWith("_BUTTON")) {
                newInteractiveBlocks.add(material);
            } else if (name.endsWith("_BED")) {
                newInteractiveBlocks.add(material);
            }
        }

        INTERACTIVE_BLOCKS = Set.copyOf(newInteractiveBlocks);
    }

    private static boolean isInteractiveBlock(Material type) {
        return INTERACTIVE_BLOCKS.contains(type);
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

    private static final Set<Material> SIGN_BLOCKS;

    static {
        List<Material> newSignBlocks = new ArrayList<>();

        for (Material material : Material.values()) {
            if (material.name().endsWith("_SIGN")) {
                newSignBlocks.add(material);
            }
        }

        SIGN_BLOCKS = Set.copyOf(newSignBlocks);
    }

    public static boolean isSign(Block block) {
        return isSign(block.getType());
    }

    public static boolean isSign(Material type) {
        return SIGN_BLOCKS.contains(type);
    }

    private static final Set<Material> WOOL;

    static {
        List<Material> newWoolBlocks = new ArrayList<>();

        for (Material material : Material.values()) {
            if (material.name().endsWith("_WOOL")) {
                newWoolBlocks.add(material);
            }
        }

        WOOL = Set.copyOf(newWoolBlocks);
    }

    public static boolean isWool(Material type) {
        return WOOL.contains(type);
    }

    public static boolean isWool(Block block) {
        return isWool(block.getType());
    }

    private static final Set<Material> CONCRETE;

    static {
        List<Material> newConcreteBlocks = new ArrayList<>();

        for (Material material : Material.values()) {
            if (material.name().endsWith("_CONCRETE")) {
                newConcreteBlocks.add(material);
            }
        }

        CONCRETE = Set.copyOf(newConcreteBlocks);
    }

    public static boolean isConcrete(Material type) {
        return CONCRETE.contains(type);
    }

    public static boolean isConcrete(Block block) {
        return isConcrete(block.getType());
    }

    private static final Set<Material> STAINED_GLASS_BLOCKS;

    static {
        List<Material> newStainedGlassBlocks = new ArrayList<>();

        for (Material material : Material.values()) {
            if (material.name().endsWith("_STAINED_GLASS")) {
                newStainedGlassBlocks.add(material);
            }
        }

        STAINED_GLASS_BLOCKS = Set.copyOf(newStainedGlassBlocks);
    }

    public static boolean isStainedGlassBlock(Material type) {
        return STAINED_GLASS_BLOCKS.contains(type);
    }

    public static boolean isStainedGlassBlock(Block block) {
        return isStainedGlassBlock(block.getType());
    }

    private static final Set<Material> STAINED_GLASS_PANES;

    static {
        List<Material> newStainedGlassPanes = new ArrayList<>();

        for (Material material : Material.values()) {
            if (material.name().endsWith("_STAINED_GLASS_PANE")) {
                newStainedGlassPanes.add(material);
            }
        }

        STAINED_GLASS_PANES = Set.copyOf(newStainedGlassPanes);
    }

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

    private static final Set<Material> BEDS;

    static {
        List<Material> newBeds = new ArrayList<>();

        for (Material material : Material.values()) {
            if (material.name().endsWith("_BED")) {
                newBeds.add(material);
            }
        }

        BEDS = Set.copyOf(newBeds);
    }

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