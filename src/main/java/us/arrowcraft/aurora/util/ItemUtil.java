package us.arrowcraft.aurora.util;
import com.sk89q.worldedit.blocks.ItemID;
import org.bukkit.ChatColor;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Author: Turtle9598
 */
public class ItemUtil {

    public static final ItemStack[] leatherArmour = new ItemStack[] {
            new ItemStack(ItemID.LEATHER_BOOTS), new ItemStack(ItemID.LEATHER_PANTS),
            new ItemStack(ItemID.LEATHER_CHEST), new ItemStack(ItemID.LEATHER_HELMET)
    };
    public static final ItemStack[] ironArmour = new ItemStack[] {
            new ItemStack(ItemID.IRON_BOOTS), new ItemStack(ItemID.IRON_PANTS),
            new ItemStack(ItemID.IRON_CHEST), new ItemStack(ItemID.IRON_HELMET)
    };
    public static final ItemStack[] goldArmour = new ItemStack[] {
            new ItemStack(ItemID.GOLD_BOOTS), new ItemStack(ItemID.GOLD_PANTS),
            new ItemStack(ItemID.GOLD_CHEST), new ItemStack(ItemID.GOLD_HELMET)
    };
    public static final ItemStack[] diamondArmour = new ItemStack[] {
            new ItemStack(ItemID.DIAMOND_BOOTS), new ItemStack(ItemID.DIAMOND_PANTS),
            new ItemStack(ItemID.DIAMOND_CHEST), new ItemStack(ItemID.DIAMOND_HELMET)
    };

    public static int countFilledSlots(ItemStack[] inventory) {

        int count = 0;
        for (ItemStack aItemStack : inventory) {
            if (aItemStack != null && aItemStack.getTypeId() != 0) count++;
        }
        return count;
    }

    public static int countItemsOfType(ItemStack[] itemStacks, int typeId) {

        int count = 0;
        for (ItemStack itemStack : itemStacks) {
            if (itemStack != null && itemStack.getTypeId() == typeId) {
                count += itemStack.getAmount();
            }
        }
        return count;
    }

    public static int countItemsOfType(ItemStack[] itemStacks, ItemStack[] checkItemStacks) {

        int count = 0;
        for (ItemStack itemStack : itemStacks) {
            for (ItemStack checkItem : checkItemStacks) {
                if (itemStack != null && itemStack.getTypeId() == checkItem.getTypeId()
                        && itemStack.getEnchantments().equals(checkItem.getEnchantments())) {
                    count++;
                }
            }
        }
        return count;
    }

    private static final int[] axes = new int[] {
            ItemID.WOOD_AXE, ItemID.STONE_AXE,
            ItemID.IRON_AXE, ItemID.DIAMOND_AXE
    };

    public static boolean isAxe(int itemId) {

        for (int axe : axes) {
            if (itemId == axe) {
                return true;
            }
        }
        return false;
    }

    private static final int[] pickAxes = new int[] {
            ItemID.WOOD_PICKAXE, ItemID.STONE_PICKAXE,
            ItemID.IRON_PICK, ItemID.DIAMOND_PICKAXE
    };

    public static boolean isPickAxe(int itemId) {

        for (int pickAxe : pickAxes) {
            if (itemId == pickAxe) {
                return true;
            }
        }
        return false;
    }

    private static final int[] ingots = new int[] {
            ItemID.IRON_BAR, ItemID.GOLD_BAR
    };

    public static boolean isIngot(int itemId) {

        for (int ingot : ingots) {
            if (itemId == ingot) {
                return true;
            }
        }
        return false;
    }

    public static boolean hasAncientArmour(Player player) {

        if (!player.isValid()) return false;

        boolean[] b = new boolean[] {false, false, false, false};
        ItemStack[] armour = player.getInventory().getArmorContents();
        for (int i = 0; i < 4; i++) {
            b[i] = armour[i] != null && armour[i].hasItemMeta() && armour[i].getItemMeta().hasDisplayName()
                    && armour[i].getItemMeta().getDisplayName().contains(ChatColor.GOLD + "Ancient");
        }
        return b[0] && b[1] && b[2] && b[3];
    }

    public static boolean hasMasterSword(Player player) {

        if (!player.isValid()) return false;

        ItemStack held = player.getInventory().getItemInHand();
        return held.hasItemMeta() && held.getItemMeta().hasDisplayName()
                && held.getItemMeta().getDisplayName().contains(ChatColor.DARK_PURPLE + "Master Sword");
    }

    public static int fortuneMultiplier(ItemStack pickaxe) {

        if (!pickaxe.containsEnchantment(Enchantment.LOOT_BONUS_BLOCKS)) return 1;
        switch (pickaxe.getEnchantmentLevel(Enchantment.LOOT_BONUS_BLOCKS)) {
            case 1:
                return 2;
            case 2:
                return 3;
            case 3:
                return 4;
            default:
                return 1;
        }
    }
}
