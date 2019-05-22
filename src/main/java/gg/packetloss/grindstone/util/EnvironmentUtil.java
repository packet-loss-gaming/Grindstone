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

  private static final Set<Integer> CROP_BLOCKS = new HashSet<>();
  private static final Set<Integer> SHRUB_BLOCKS = new HashSet<>();
  private static final Set<Integer> INVALUABLE_ORES = new HashSet<>();
  private static final Set<Integer> VALUABLE_BLOCKS = new HashSet<>();
  private static final Set<Integer> VALUABLE_ORES = new HashSet<>();
  private static final Set<Integer> CONTAINER_BLOCKS = new HashSet<>();
  private static final Set<Integer> INTERACTIVE_BLOCKS = new HashSet<>();
  private static final Set<Integer> SIGN_BLOCKS = new HashSet<>();
  private static final Set<Integer> WATER_BLOCKS = new HashSet<>();
  private static final Set<Integer> LAVA_BLOCKS = new HashSet<>();
  private static final Set<Biome> FROZEN_BIOMES = new HashSet<>();
  private static final BlockFace[] NEARBY = new BlockFace[] {
      BlockFace.NORTH, BlockFace.NORTH_EAST, BlockFace.EAST, BlockFace.NORTH_WEST,
      BlockFace.SOUTH, BlockFace.SOUTH_EAST, BlockFace.WEST, BlockFace.SOUTH_WEST,
      BlockFace.SELF
  };
  private static Set<EntityType> HOSTILE_ENTITIES = new HashSet<>();

  static {
    CROP_BLOCKS.add(BlockID.CROPS);
    CROP_BLOCKS.add(BlockID.MELON_STEM);
    CROP_BLOCKS.add(BlockID.CARROTS);
    CROP_BLOCKS.add(BlockID.POTATOES);
    CROP_BLOCKS.add(BlockID.PUMPKIN_STEM);
  }

  static {
    SHRUB_BLOCKS.add(BlockID.DEAD_BUSH);
    SHRUB_BLOCKS.add(BlockID.LONG_GRASS);
    SHRUB_BLOCKS.add(BlockID.RED_FLOWER);
    SHRUB_BLOCKS.add(BlockID.YELLOW_FLOWER);
    SHRUB_BLOCKS.add(BlockID.DOUBLE_PLANT);
    SHRUB_BLOCKS.add(BlockID.RED_MUSHROOM);
    SHRUB_BLOCKS.add(BlockID.BROWN_MUSHROOM);
  }

  static {
    INVALUABLE_ORES.add(BlockID.COAL_ORE);
  }

  static {
    VALUABLE_BLOCKS.add(BlockID.GOLD_BLOCK);
    VALUABLE_BLOCKS.add(BlockID.LAPIS_LAZULI_BLOCK);
    VALUABLE_BLOCKS.add(BlockID.IRON_BLOCK);
    VALUABLE_BLOCKS.add(BlockID.DIAMOND_BLOCK);
    VALUABLE_BLOCKS.add(BlockID.EMERALD_BLOCK);
    VALUABLE_BLOCKS.add(BlockID.BEACON);
    VALUABLE_BLOCKS.add(BlockID.COMMAND_BLOCK);
    VALUABLE_BLOCKS.add(BlockID.COAL_BLOCK);
  }

  static {
    VALUABLE_ORES.add(BlockID.GOLD_ORE);
    VALUABLE_ORES.add(BlockID.LAPIS_LAZULI_ORE);
    VALUABLE_ORES.add(BlockID.IRON_ORE);
    VALUABLE_ORES.add(BlockID.DIAMOND_ORE);
    VALUABLE_ORES.add(BlockID.REDSTONE_ORE);
    VALUABLE_ORES.add(BlockID.GLOWING_REDSTONE_ORE);
    VALUABLE_ORES.add(BlockID.EMERALD_ORE);
    VALUABLE_ORES.add(BlockID.QUARTZ_ORE);
  }

  static {
    CONTAINER_BLOCKS.add(BlockID.BREWING_STAND);
    CONTAINER_BLOCKS.add(BlockID.CHEST);
    CONTAINER_BLOCKS.add(BlockID.DISPENSER);
    CONTAINER_BLOCKS.add(BlockID.DROPPER);
    CONTAINER_BLOCKS.add(BlockID.FURNACE);
    CONTAINER_BLOCKS.add(BlockID.BURNING_FURNACE);
    CONTAINER_BLOCKS.add(BlockID.JUKEBOX);
    CONTAINER_BLOCKS.add(BlockID.ENDER_CHEST);
    CONTAINER_BLOCKS.add(BlockID.TRAPPED_CHEST);
    CONTAINER_BLOCKS.add(BlockID.HOPPER);
  }

  static {
    INTERACTIVE_BLOCKS.add(BlockID.WORKBENCH);
    INTERACTIVE_BLOCKS.add(BlockID.ENCHANTMENT_TABLE);
    INTERACTIVE_BLOCKS.add(BlockID.BEACON);
    INTERACTIVE_BLOCKS.add(BlockID.ANVIL);
    INTERACTIVE_BLOCKS.add(BlockID.LEVER);
    INTERACTIVE_BLOCKS.add(BlockID.STONE_BUTTON);
    INTERACTIVE_BLOCKS.add(BlockID.WOODEN_BUTTON);
    INTERACTIVE_BLOCKS.add(BlockID.WOODEN_DOOR);
    INTERACTIVE_BLOCKS.add(BlockID.FENCE_GATE);
    INTERACTIVE_BLOCKS.add(BlockID.TRAP_DOOR);
  }

  static {
    SIGN_BLOCKS.add(BlockID.SIGN_POST);
    SIGN_BLOCKS.add(BlockID.WALL_SIGN);
  }

  static {
    WATER_BLOCKS.add(BlockID.WATER);
    WATER_BLOCKS.add(BlockID.STATIONARY_WATER);
  }

  static {
    LAVA_BLOCKS.add(BlockID.LAVA);
    LAVA_BLOCKS.add(BlockID.STATIONARY_LAVA);
  }

  static {
    FROZEN_BIOMES.add(Biome.FROZEN_OCEAN);
    FROZEN_BIOMES.add(Biome.FROZEN_RIVER);
    FROZEN_BIOMES.add(Biome.ICE_MOUNTAINS);
    FROZEN_BIOMES.add(Biome.ICE_PLAINS);
    FROZEN_BIOMES.add(Biome.ICE_PLAINS_SPIKES);
  }

  static {
    HOSTILE_ENTITIES.add(EntityType.ENDERMAN);
    HOSTILE_ENTITIES.add(EntityType.PIG_ZOMBIE);
    HOSTILE_ENTITIES.add(EntityType.ZOMBIE);
    HOSTILE_ENTITIES.add(EntityType.SKELETON);
    HOSTILE_ENTITIES.add(EntityType.CREEPER);
    HOSTILE_ENTITIES.add(EntityType.SILVERFISH);
    HOSTILE_ENTITIES.add(EntityType.SPIDER);
    HOSTILE_ENTITIES.add(EntityType.CAVE_SPIDER);
    HOSTILE_ENTITIES.add(EntityType.SLIME);
    HOSTILE_ENTITIES.add(EntityType.MAGMA_CUBE);
    HOSTILE_ENTITIES.add(EntityType.BLAZE);
    HOSTILE_ENTITIES.add(EntityType.WITCH);
  }

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
    if (t < 0) {
      t += 2;
    }
    return (t == 1);
  }

  public static boolean isMidnight(long time) {

    return time == ((0 - 8 + 24) * 1000);
  }

  public static boolean isCropBlock(Block block) {

    return isCropBlock(block.getTypeId());
  }

  public static boolean isCropBlock(int block) {

    return CROP_BLOCKS.contains(block);
  }

  public static boolean isShrubBlock(Block block) {

    return isShrubBlock(block.getTypeId());
  }

  public static boolean isShrubBlock(int block) {

    return SHRUB_BLOCKS.contains(block) || isCropBlock(block);
  }

  public static boolean isOre(Block block) {

    return isOre(block.getTypeId());
  }

  public static boolean isOre(int block) {

    return INVALUABLE_ORES.contains(block) || isValuableOre(block);
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

  public static boolean isValuableBlock(int block) {

    return VALUABLE_BLOCKS.contains(block);
  }

  public static boolean isValuableOre(Block block) {

    return isValuableOre(block.getTypeId());
  }

  public static boolean isValuableOre(int block) {

    return VALUABLE_ORES.contains(block);
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

  public static boolean isContainer(Block block) {

    return isContainer(block.getTypeId());
  }

  public static boolean isContainer(int block) {

    return CONTAINER_BLOCKS.contains(block);
  }

  private static boolean isInteractiveBlock(int block) {

    return INTERACTIVE_BLOCKS.contains(block) || isContainer(block);
  }

  public static boolean isInteractiveBlock(Block block) {
    if (isInteractiveBlock(block.getTypeId())) {
      return true;
    }

    if (block.getState() instanceof Sign) {
      Sign signState = (Sign) block.getState();
      for (String line : signState.getLines()) {
        if (line.matches("\\[.*\\]")) {
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

    return EnvironmentUtil.isInteractiveBlock(clicked.getRelative(clickedFace));

  }

  public static boolean isSign(Block block) {

    return isSign(block.getTypeId());
  }

  public static boolean isSign(int block) {

    return SIGN_BLOCKS.contains(block);
  }

  public static boolean isWater(Block block) {

    return isWater(block.getTypeId());
  }

  public static boolean isWater(int block) {

    return WATER_BLOCKS.contains(block);
  }

  public static boolean isLava(Block block) {

    return isLava(block.getTypeId());
  }

  public static boolean isLava(int block) {

    return LAVA_BLOCKS.contains(block);
  }

  public static boolean isLiquid(int block) {

    return isWater(block) || isLava(block);
  }

  public static boolean isFrozenBiome(Biome biome) {

    return FROZEN_BIOMES.contains(biome);
  }

  public static void generateRadialEffect(Location location, Effect effect) {

    for (int i = 0; i < 20; i++) {
      location.getWorld().playEffect(location, effect, ChanceUtil.getRandom(9) - 1);
    }
  }

  public static void generateRadialEffect(Location[] locations, Effect effect) {

    for (Location loc : locations) {
      generateRadialEffect(loc, effect);
    }
  }

  public static boolean isHostileEntity(Entity e) {

    return e != null && e.isValid() && e.getType() != null && HOSTILE_ENTITIES.contains(e.getType());
  }

  public static BlockFace[] getNearbyBlockFaces() {

    return NEARBY;
  }
}