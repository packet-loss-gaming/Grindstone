package com.skelril.aurora.util.item;
import com.sk89q.worldedit.blocks.ItemID;
import org.bukkit.ChatColor;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.Repairable;

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

    public static class Master {

        public static ItemStack makeBow() {

            ItemStack masterBow = new ItemStack(ItemID.BOW);
            ItemMeta masterMeta = masterBow.getItemMeta();
            masterMeta.addEnchant(Enchantment.ARROW_DAMAGE, 10, true);
            masterMeta.addEnchant(Enchantment.ARROW_FIRE, 10, true);
            masterMeta.addEnchant(Enchantment.ARROW_INFINITE, 1, true);
            masterMeta.addEnchant(Enchantment.ARROW_KNOCKBACK, 10, true);
            masterMeta.addEnchant(Enchantment.LOOT_BONUS_MOBS, 10, true);
            masterMeta.setDisplayName(ChatColor.DARK_PURPLE + "Master Bow");
            ((Repairable) masterMeta).setRepairCost(400);
            masterBow.setItemMeta(masterMeta);
            return masterBow;
        }

        public static ItemStack makeSword() {

            ItemStack masterSword = new ItemStack(ItemID.DIAMOND_SWORD);
            ItemMeta masterMeta = masterSword.getItemMeta();
            masterMeta.addEnchant(Enchantment.DAMAGE_ALL, 10, true);
            masterMeta.addEnchant(Enchantment.DAMAGE_ARTHROPODS, 10, true);
            masterMeta.addEnchant(Enchantment.DAMAGE_UNDEAD, 10, true);
            masterMeta.addEnchant(Enchantment.FIRE_ASPECT, 10, true);
            masterMeta.addEnchant(Enchantment.KNOCKBACK, 10, true);
            masterMeta.addEnchant(Enchantment.LOOT_BONUS_MOBS, 10, true);
            masterMeta.setDisplayName(ChatColor.DARK_PURPLE + "Master Sword");
            ((Repairable) masterMeta).setRepairCost(400);
            masterSword.setItemMeta(masterMeta);
            return masterSword;
        }
    }

    public static class Unleashed {

        public static ItemStack makeSword() {

            ItemStack unleashedSword = Master.makeSword();
            ItemMeta unleashedMeta = unleashedSword.getItemMeta();
            unleashedMeta.setDisplayName(ChatColor.DARK_PURPLE + "Unleashed Sword");
            unleashedSword.setItemMeta(unleashedMeta);
            return unleashedSword;
        }

        public static ItemStack makeBow() {

            ItemStack unleashedBow = Master.makeBow();
            ItemMeta unleashedMeta = unleashedBow.getItemMeta();
            unleashedMeta.setDisplayName(ChatColor.DARK_PURPLE + "Unleashed Bow");
            unleashedBow.setItemMeta(unleashedMeta);
            return unleashedBow;
        }
    }

    public static class Fear {

        public static ItemStack makeSword() {

            ItemStack fearSword = new ItemStack(ItemID.DIAMOND_SWORD);
            ItemMeta fearMeta = fearSword.getItemMeta();
            fearMeta.addEnchant(Enchantment.DAMAGE_ALL, 7, true);
            fearMeta.addEnchant(Enchantment.DAMAGE_ARTHROPODS, 7, true);
            fearMeta.addEnchant(Enchantment.DAMAGE_UNDEAD, 7, true);
            fearMeta.addEnchant(Enchantment.FIRE_ASPECT, 7, true);
            fearMeta.addEnchant(Enchantment.KNOCKBACK, 7, true);
            fearMeta.addEnchant(Enchantment.LOOT_BONUS_MOBS, 7, true);
            fearMeta.setDisplayName(ChatColor.DARK_RED + "Fear Sword");
            ((Repairable) fearMeta).setRepairCost(400);
            fearSword.setItemMeta(fearMeta);
            return fearSword;
        }

        public static ItemStack makeBow() {

            ItemStack fearBow = new ItemStack(ItemID.BOW);
            ItemMeta fearMeta = fearBow.getItemMeta();
            fearMeta.addEnchant(Enchantment.ARROW_DAMAGE, 7, true);
            fearMeta.addEnchant(Enchantment.ARROW_FIRE, 7, true);
            fearMeta.addEnchant(Enchantment.ARROW_INFINITE, 1, true);
            fearMeta.addEnchant(Enchantment.ARROW_KNOCKBACK, 7, true);
            fearMeta.addEnchant(Enchantment.LOOT_BONUS_MOBS, 7, true);
            fearMeta.setDisplayName(ChatColor.DARK_RED + "Fear Bow");
            ((Repairable) fearMeta).setRepairCost(400);
            fearBow.setItemMeta(fearMeta);
            return fearBow;
        }
    }

    public static class GraveYard {

        public static ItemStack gemOfDarkness(int amount) {

            ItemStack gemOfDarkness = new ItemStack(ItemID.EMERALD, amount);
            ItemMeta gemMeta = gemOfDarkness.getItemMeta();
            gemMeta.setDisplayName(ChatColor.DARK_RED + "Gem of Darkness");
            gemOfDarkness.setItemMeta(gemMeta);
            return gemOfDarkness;
        }

        public static ItemStack phantomGold(int amount) {

            ItemStack phantomGold = new ItemStack(ItemID.GOLD_BAR, amount);
            ItemMeta goldMeta = phantomGold.getItemMeta();
            goldMeta.setDisplayName(ChatColor.GOLD + "Phantom Gold");
            phantomGold.setItemMeta(goldMeta);
            return phantomGold;
        }

        public static ItemStack imbuedCrystal(int amount) {

            ItemStack gemOfDarkness = new ItemStack(ItemID.DIAMOND, amount);
            ItemMeta gemMeta = gemOfDarkness.getItemMeta();
            gemMeta.setDisplayName(ChatColor.AQUA + "Imbued Crystal");
            gemOfDarkness.setItemMeta(gemMeta);
            return gemOfDarkness;
        }

        public static ItemStack batBow() {

            ItemStack batBow = new ItemStack(ItemID.BOW);
            ItemMeta batMeta = batBow.getItemMeta();
            batMeta.setDisplayName(ChatColor.DARK_RED + "Bat Bow");
            ((Repairable) batMeta).setRepairCost(400);
            batBow.setItemMeta(batMeta);
            return batBow;
        }

        public static ItemStack chickenBow() {

            ItemStack chickenBow = new ItemStack(ItemID.BOW);
            ItemMeta chickenMeta = chickenBow.getItemMeta();
            chickenMeta.setDisplayName(ChatColor.DARK_RED + "Chicken Bow");
            ((Repairable) chickenMeta).setRepairCost(400);
            chickenBow.setItemMeta(chickenMeta);
            return chickenBow;
        }
    }

    public static ItemStack[] clone(ItemStack[] stacks) {

        ItemStack[] returnStack = new ItemStack[stacks.length];
        for (int i = 0; i < stacks.length; i++) {
            returnStack[i] = stacks[i] == null ? null : stacks[i].clone();
        }
        return returnStack;
    }

    public static int countFilledSlots(Player player) {

        PlayerInventory inventory = player.getInventory();
        return countFilledSlots(inventory.getContents()) + countFilledSlots(inventory.getArmorContents());
    }

    public static int countFilledSlots(ItemStack[] inventory) {

        int count = 0;
        for (ItemStack aItemStack : inventory) {
            if (aItemStack != null && aItemStack.getTypeId() != 0) count++;
        }
        return count;
    }

    public static boolean findItemOfName(ItemStack[] itemStacks, String name) {

        for (ItemStack itemStack : itemStacks) {
            if (matchesFilter(itemStack, name)) return true;
        }
        return false;
    }

    public static int countItemsOfName(ItemStack[] itemStacks, String name) {

        int count = 0;
        for (ItemStack itemStack : itemStacks) {
            if (matchesFilter(itemStack, name)) count++;
        }
        return count;
    }


    public static int countItemsOfType(ItemStack[] itemStacks, int typeId) {

        int count = 0;
        for (ItemStack itemStack : itemStacks) {
            if (itemStack != null && !isNamed(itemStack) && itemStack.getTypeId() == typeId) {
                count += itemStack.getAmount();
            }
        }
        return count;
    }

    public static int countItemsOfType(ItemStack[] itemStacks, ItemStack[] checkItemStacks) {

        int count = 0;
        for (ItemStack itemStack : itemStacks) {
            for (ItemStack checkItem : checkItemStacks) {
                if (itemStack != null && !isNamed(itemStack) && itemStack.equals(checkItem)) {
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

    public static boolean isPhantomGold(ItemStack stack) {

        return matchesFilter(stack, ChatColor.GOLD + "Phantom Gold");
    }

    public static boolean hasAncientArmour(Player player) {

        if (!player.isValid()) return false;

        boolean[] b = new boolean[] {false, false, false, false};
        ItemStack[] armour = player.getInventory().getArmorContents();
        for (int i = 0; i < 4; i++) {
            b[i] = matchesFilter(armour[i], ChatColor.GOLD + "Ancient");
        }
        return b[0] && b[1] && b[2] && b[3];
    }

    private static boolean isNamed(ItemStack stack) {

        return stack != null && stack.hasItemMeta() && stack.getItemMeta().hasDisplayName();
    }

    public static boolean matchesFilter(ItemStack stack, String filter) {

        return isNamed(stack) && stack.getItemMeta().getDisplayName().startsWith(filter);
    }

    public static boolean hasBatBow(Player player) {

        return player.isValid() && isBatBow(player.getItemInHand());
    }

    public static boolean isBatBow(ItemStack item) {

        return matchesFilter(item, ChatColor.DARK_RED + "Bat Bow");
    }

    public static boolean hasChickenBow(Player player) {

        return player.isValid() && isChickenBow(player.getItemInHand());
    }

    public static boolean isChickenBow(ItemStack item) {

        return matchesFilter(item, ChatColor.DARK_RED + "Chicken Bow");
    }

    public static boolean hasMasterSword(Player player) {

        return player.isValid() && isMasterSword(player.getItemInHand());
    }

    public static boolean isMasterSword(ItemStack item) {

        return matchesFilter(item, ChatColor.DARK_PURPLE + "Master Sword");
    }

    public static boolean hasMasterBow(Player player) {

        return player.isValid() && isMasterBow(player.getItemInHand());
    }

    public static boolean isMasterBow(ItemStack item) {

        return matchesFilter(item, ChatColor.DARK_PURPLE + "Master Bow");
    }

    public static boolean hasUnleashedSword(Player player) {

        return player.isValid() && isUnleashedSword(player.getItemInHand());
    }

    public static boolean isUnleashedSword(ItemStack item) {

        return matchesFilter(item, ChatColor.DARK_PURPLE + "Unleashed Sword");
    }

    public static boolean hasUnleashedBow(Player player) {

        return player.isValid() && isUnleashedBow(player.getItemInHand());
    }

    public static boolean isUnleashedBow(ItemStack item) {

        return matchesFilter(item, ChatColor.DARK_PURPLE + "Unleashed Bow");
    }

    public static boolean hasFearHelmet(Player player) {

        return player.isValid() && isFearHelmet(player.getInventory().getHelmet());
    }

    public static boolean isFearHelmet(ItemStack item) {

        return matchesFilter(item, ChatColor.DARK_RED + "Fear Helmet");
    }

    public static boolean hasFearSword(Player player) {

        return player.isValid() && isFearSword(player.getItemInHand());
    }

    public static boolean isFearSword(ItemStack item) {

        return matchesFilter(item, ChatColor.DARK_RED + "Fear Sword");
    }

    public static boolean hasFearBow(Player player) {

        return player.isValid() && isFearBow(player.getItemInHand());
    }

    public static boolean isFearBow(ItemStack item) {

        return matchesFilter(item, ChatColor.DARK_RED + "Fear Bow");
    }

    public static boolean hasForgeBook(Player player) {

        return player.isValid() && hasForgeBook(player.getItemInHand());
    }

    public static boolean hasForgeBook(ItemStack item) {

        return item.hasItemMeta() && item.getItemMeta() instanceof BookMeta
                && ((BookMeta) item.getItemMeta()).hasAuthor()
                && ((BookMeta) item.getItemMeta()).getAuthor().equals("The Forge Knights");
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
