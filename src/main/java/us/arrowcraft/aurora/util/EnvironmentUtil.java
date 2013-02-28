package us.arrowcraft.aurora.util;

import com.sk89q.worldedit.blocks.BlockID;
import com.sk89q.worldedit.blocks.ItemID;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * @author Turtle9598
 */
public class EnvironmentUtil {

    private final static Random random = new Random();

    public static boolean isNightTime(long time) {

        long day = ((24) * 1000);
        long night = ((22 - 8 + 24) * 1000);
        return (time >= night) || (time <= day);
    }

    public static boolean isDayTime(long time) {

        return !isNightTime(time);
    }

    public static boolean isServerTimeOdd(long time) {

        long t = time % 2;
        if (t < 0)
            t += 2;
        return (t == 1);
    }

    public static boolean isMidnight(long time) {

        return time == ((0 - 8 + 24) * 1000);
    }

    private static final int[] cropBlocks = new int[] {
            BlockID.CROPS, BlockID.MELON_STEM,
            BlockID.CARROTS, BlockID.POTATOES,
            BlockID.PUMPKIN_STEM
    };

    public static boolean isCropBlock(Block block) {

        return isCropBlock(block.getTypeId());
    }

    public static boolean isCropBlock(int block) {

        for (int cropBlock : cropBlocks) {
            if (cropBlock == block) {
                return true;
            }
        }

        return false;
    }

    private static final int[] shrubBlocks = new int[] {
            BlockID.LONG_GRASS, BlockID.RED_FLOWER, BlockID.YELLOW_FLOWER
    };

    public static boolean isShrubBlock(Block block) {

        return isShrubBlock(block.getTypeId());
    }

    public static boolean isShrubBlock(int block) {

        for (int aShrubBlock : shrubBlocks) {
            if (aShrubBlock == block) return true;
        }
        return false;
    }

    private static final int[] shrubBlockDrops = new int[] {
            ItemID.SEEDS, BlockID.RED_FLOWER, BlockID.YELLOW_FLOWER
    };

    public static boolean isShrubBlockDrop(int block) {

        for (int aShrubBlockDrop : shrubBlockDrops) {
            if (aShrubBlockDrop == block) return true;
        }
        return false;
    }

    private static final int[] invaluableOres = new int[] {
            BlockID.COAL_ORE
    };

    public static boolean isOre(Block block) {

        return isOre(block.getTypeId());
    }

    public static boolean isOre(int block) {

        for (int ore : invaluableOres) {
            if (ore == block) return true;
        }
        return isValuableOre(block);
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

    private static final int[] valuableBlocks = new int[] {
            BlockID.GOLD_BLOCK, BlockID.LAPIS_LAZULI_BLOCK,
            BlockID.IRON_BLOCK, BlockID.DIAMOND_BLOCK,
            BlockID.EMERALD_BLOCK, BlockID.BEACON,
            BlockID.COMMAND_BLOCK
    };

    public static boolean isValuableBlock(int block) {

        for (int valuableBlock : valuableBlocks) {
            if (valuableBlock == block) {
                return true;
            }
        }
        return false;
    }

    private static final int[] valuableOres = new int[] {
            BlockID.GOLD_ORE, BlockID.LAPIS_LAZULI_ORE,
            BlockID.IRON_ORE, BlockID.DIAMOND_ORE,
            BlockID.REDSTONE_ORE, BlockID.GLOWING_REDSTONE_ORE,
            BlockID.EMERALD_ORE
    };

    public static boolean isValuableOre(Block block) {

        return isValuableOre(block.getTypeId());
    }

    public static boolean isValuableOre(int block) {

        for (int valuableOre : valuableOres) {
            if (valuableOre == block) {
                return true;
            }
        }
        return false;
    }

    public static ItemStack getOreDrop(Block block, boolean hasSilkTouch) {

        return getOreDrop(block.getTypeId(), hasSilkTouch);
    }

    public static ItemStack getOreDrop(int block, boolean hasSilkTouch) {

        if (!isOre(block)) {
            return null;
        } else if (hasSilkTouch) {
            return new ItemStack(block);
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
                default:
                    return null;
            }
        }
    }

    private static final int[] containerBlocks = new int[] {
            BlockID.BREWING_STAND, BlockID.CHEST,
            BlockID.DISPENSER, BlockID.FURNACE,
            BlockID.BURNING_FURNACE, BlockID.JUKEBOX,
            BlockID.ENDER_CHEST
    };

    public static boolean isContainer(Block block) {

        return isContainer(block.getTypeId());
    }

    public static boolean isContainer(int block) {

        for (int containerBlock : containerBlocks) {
            if (containerBlock == block) {
                return true;
            }
        }
        return false;
    }

    private static final int[] interactiveBlocks = new int[] {
            BlockID.WORKBENCH, BlockID.ENCHANTMENT_TABLE,
            BlockID.BEACON, BlockID.ANVIL
    };

    public static boolean isInteractiveBlock(Block block) {

        return isInteractiveBlock(block.getTypeId());
    }

    public static boolean isInteractiveBlock(int block) {

        for (int interactiveBlock : interactiveBlocks) {
            if (interactiveBlock == block) {
                return true;
            }
        }
        return isContainer(block);
    }

    private static final int[] signBlocks = new int[] {
            BlockID.SIGN_POST, BlockID.WALL_SIGN
    };

    public static boolean isSign(Block block) {

        return isSign(block.getTypeId());
    }

    public static boolean isSign(int block) {

        for (int sign : signBlocks) {
            if (sign == block) {
                return true;
            }
        }
        return false;
    }

    private static final int[] waterBlocks = new int[] {
            BlockID.WATER, BlockID.STATIONARY_WATER
    };

    public static boolean isWater(Block block) {

        return isWater(block.getTypeId());
    }

    public static boolean isWater(int block) {

        for (int water : waterBlocks) {
            if (water == block) {
                return true;
            }
        }
        return false;
    }

    private static final Biome[] frozenBiomes = new Biome[] {
            Biome.FROZEN_OCEAN, Biome.FROZEN_RIVER,
            Biome.ICE_MOUNTAINS, Biome.ICE_PLAINS
    };

    public static boolean isFrozenBiome(Biome biome) {

        for (Biome aFrozenBiome : frozenBiomes) {
            if (aFrozenBiome == biome) return true;
        }
        return false;
    }

    public static void generateRadialEffect(Location location, Effect effect) {

        for (int i = 0; i < 100; i++) {
            location.getWorld().playEffect(location, effect, random.nextInt(9));
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
}