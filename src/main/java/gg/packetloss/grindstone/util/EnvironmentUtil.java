/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.util;

import gg.packetloss.grindstone.util.item.ItemUtil;
import org.apache.commons.lang.Validate;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static gg.packetloss.grindstone.util.MaterialUtil.generatePostfixMaterialSet;

public class EnvironmentUtil {
    private static final long START_NIGHT = 12 * 1000;

    public static long getNightStartTime() {
        return START_NIGHT;
    }

    public static boolean isNightTime(long time) {
        return !isDayTime(time);
    }

    public static boolean isDayTime(long time) {
        return time < START_NIGHT;
    }

    public static boolean isServerTimeOdd(long time) {
        long t = time % 2;
        if (t < 0) t += 2;
        return (t == 1);
    }

    public static boolean hasThunderstorm(World world) {
        return world.hasStorm() && world.isThundering();
    }

    public static boolean isAirBlock(Material type) {
        return type.isAir();
    }
    public static boolean isAirBlock(Block block) {
        return isAirBlock(block.getType());
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

    private static final Set<Material> SHRUB_BLOCKS;

    static {
        List<Material> newShrubBlocks = new ArrayList<>(List.of(
            Material.DEAD_BUSH, Material.SHORT_GRASS, Material.TALL_GRASS,
            Material.RED_MUSHROOM, Material.BROWN_MUSHROOM
        ));

        newShrubBlocks.addAll(Tag.FLOWERS.getValues());

        SHRUB_BLOCKS = Set.copyOf(newShrubBlocks);
    }

    public static boolean isShrubBlock(Block block) {
        return isShrubBlock(block.getType());
    }

    public static boolean isShrubBlock(Material type) {
        return SHRUB_BLOCKS.contains(type) || isCropBlock(type);
    }

    private static final Set<Material> ORES = generatePostfixMaterialSet("_ORE");

    public static boolean isOre(Block block) {
        return isOre(block.getType());
    }

    public static boolean isOre(Material type) {
        return ORES.contains(type);
    }

    private static ItemStack getOreDrop(Material block) {
        if (Tag.COAL_ORES.isTagged(block)) {
            return new ItemStack(Material.COAL);
        }
        if (Tag.IRON_ORES.isTagged(block)) {
            return new ItemStack(Material.RAW_IRON);
        }
        if (Tag.COPPER_ORES.isTagged(block)) {
            return new ItemStack(Material.RAW_COPPER, ChanceUtil.getRangedRandom(2, 3));
        }
        if (Tag.LAPIS_ORES.isTagged(block)) {
            return new ItemStack(Material.LAPIS_LAZULI, ChanceUtil.getRangedRandom(4, 9));
        }
        if (Tag.REDSTONE_ORES.isTagged(block)) {
            return new ItemStack(Material.REDSTONE, ChanceUtil.getRangedRandom(4, 5));
        }
        if (Tag.GOLD_ORES.isTagged(block)) {
            if (block == Material.NETHER_GOLD_ORE) {
                return new ItemStack(Material.GOLD_NUGGET, ChanceUtil.getRangedRandom(2, 6));
            }
            return new ItemStack(Material.RAW_GOLD);
        }
        if (Tag.DIAMOND_ORES.isTagged(block)) {
            return new ItemStack(Material.DIAMOND);
        }
        if (Tag.EMERALD_ORES.isTagged(block)) {
            return new ItemStack(Material.EMERALD);
        }
        if (block == Material.NETHER_QUARTZ_ORE) {
            return new ItemStack(Material.QUARTZ);
        }

        throw new UnsupportedOperationException("Unknown ore: " + block.name());
    }

    public static ItemStack getOreDrop(Material block, ItemStack tool) {
        Validate.isTrue(isOre(block));

        if (tool.containsEnchantment(Enchantment.SILK_TOUCH)) {
            return new ItemStack(block);
        } else {
            int fortuneModifier = ItemUtil.fortuneModifier(block, ItemUtil.fortuneLevel(tool));
            ItemStack stack = getOreDrop(block);
            stack.setAmount(stack.getAmount() * fortuneModifier);
            return stack;
        }
    }

    static {
        // Do a runtime validation of the getOreDrop function to make sure it's setup correctly.
        for (Material ore : ORES) {
            ItemStack result = getOreDrop(ore, new ItemStack(Material.DIAMOND_PICKAXE));
            Validate.notNull(result);

            Material resultType = result.getType();
            Validate.isTrue(resultType != ore);
        }
    }

    private static final Set<Material> LOGS = Tag.LOGS.getValues();

    public static boolean isLog(Block block) {
        return isLog(block.getType());
    }

    public static boolean isLog(Material type) {
        return LOGS.contains(type);
    }

    private static final Set<Material> SAPLINGS = Tag.SAPLINGS.getValues();

    public static boolean isSapling(Block block) {
        return isSapling(block.getType());
    }

    public static boolean isSapling(Material type) {
        return SAPLINGS.contains(type);
    }

    private static final Set<Material> PORTABLE_CONTAINER_BLOCKS;

    static {
        List<Material> newPortableContainerBlocks = new ArrayList<>(List.of(
            Material.SHULKER_BOX, Material.ENDER_CHEST
        ));

        newPortableContainerBlocks.addAll(Tag.SHULKER_BOXES.getValues());

        PORTABLE_CONTAINER_BLOCKS = Set.copyOf(newPortableContainerBlocks);
    }

    public static boolean isPortableContainer(Block block) {
        return isPortableContainer(block.getType());
    }

    public static boolean isPortableContainer(Material type) {
        return PORTABLE_CONTAINER_BLOCKS.contains(type);
    }

    private static final Set<Material> CONTAINER_BLOCKS;

    static {
        List<Material> newContainerBlocks = new ArrayList<>(List.of(
            Material.BREWING_STAND, Material.CHEST, Material.DISPENSER, Material.DROPPER, Material.FURNACE,
            Material.JUKEBOX, Material.TRAPPED_CHEST, Material.HOPPER, Material.BARREL
        ));

        newContainerBlocks.addAll(PORTABLE_CONTAINER_BLOCKS);

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

    private static final Set<Material> TRAPDOOR_BLOCKS = Tag.TRAPDOORS.getValues();

    public static boolean isTrapdoorBlock(Material type) {
        return TRAPDOOR_BLOCKS.contains(type);
    }

    public static boolean isTrapdoorBlock(Block block) {
        return isTrapdoorBlock(block.getType());
    }

    private static final Set<Material> CLOSEABLE_BLOCKS;

    static {
        List<Material> newClosableBlocks = new ArrayList<>();

        newClosableBlocks.addAll(Tag.WOODEN_DOORS.getValues());
        newClosableBlocks.addAll(Tag.FENCE_GATES.getValues());

        newClosableBlocks.addAll(TRAPDOOR_BLOCKS);

        CLOSEABLE_BLOCKS = Set.copyOf(newClosableBlocks);
    }

    public static boolean isClosable(Material type) {
        return CLOSEABLE_BLOCKS.contains(type);
    }

    public static boolean isClosable(Block block) {
        return isClosable(block.getType());
    }

    public static boolean isLadder(Material type) {
        return type == Material.LADDER;
    }

    public static boolean isLadder(Block block) {
        return isLadder(block.getType());
    }

    private static final Set<Material> BEDS = Tag.BEDS.getValues();

    public static boolean isBed(Material type) {
        return BEDS.contains(type);
    }

    public static boolean isBed(Block block) {
        return isBed(block.getType());
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
        newInteractiveBlocks.addAll(BEDS);
        newInteractiveBlocks.addAll(Tag.BUTTONS.getValues());

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

        if (EnvironmentUtil.isInteractiveBlock(clicked.getRelative(clickedFace.getOppositeFace()))) {
            return true;
        }

        return false;
    }

    private static final Set<Material> SIGN_BLOCKS = Tag.SIGNS.getValues();

    public static boolean isSign(Block block) {
        return isSign(block.getType());
    }

    public static boolean isSign(Material type) {
        return SIGN_BLOCKS.contains(type);
    }

    private static final Set<Material> WOOL = Tag.WOOL.getValues();

    public static boolean isWool(Material type) {
        return WOOL.contains(type);
    }

    public static boolean isWool(Block block) {
        return isWool(block.getType());
    }

    private static final Set<Material> CONCRETE = generatePostfixMaterialSet("_CONCRETE");

    public static boolean isConcrete(Material type) {
        return CONCRETE.contains(type);
    }

    public static boolean isConcrete(Block block) {
        return isConcrete(block.getType());
    }

    private static final Set<Material> STAINED_GLASS_BLOCKS = generatePostfixMaterialSet("_STAINED_GLASS");

    public static boolean isStainedGlassBlock(Material type) {
        return STAINED_GLASS_BLOCKS.contains(type);
    }

    public static boolean isStainedGlassBlock(Block block) {
        return isStainedGlassBlock(block.getType());
    }

    private static final Set<Material> STAINED_GLASS_PANES = generatePostfixMaterialSet("_STAINED_GLASS_PANE");

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

    public static boolean isLiquid(Block block) {
        return isLiquid(block.getType());
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

    public static boolean isSolidBlock(Material material) {
        if (!material.isSolid()) {
            return false;
        }

        if (isSign(material)) {
            return false;
        }

        return true;
    }

    public static boolean isSolidBlock(Block block) {
        return isSolidBlock(block.getType());
    }

    public static void generateRadialEffect(Location location, Effect effect) {

        for (int i = 0; i < 20; i++) {
            location.getWorld().playEffect(location, effect, ChanceUtil.getRandom(9) - 1);
        }
    }

    public static void generateRadialEffect(Location[] locations, Effect effect) {

        for (Location loc : locations) generateRadialEffect(loc, effect);
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