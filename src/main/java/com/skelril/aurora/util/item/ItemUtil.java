package com.skelril.aurora.util.item;

import com.sk89q.worldedit.blocks.BlockID;
import com.sk89q.worldedit.blocks.ItemID;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.inventory.meta.Repairable;
import org.bukkit.potion.Potion;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;

/**
 * Author: Turtle9598
 */
public class ItemUtil {

    public static final ItemStack[] leatherArmour = new ItemStack[]{
            new ItemStack(ItemID.LEATHER_BOOTS), new ItemStack(ItemID.LEATHER_PANTS),
            new ItemStack(ItemID.LEATHER_CHEST), new ItemStack(ItemID.LEATHER_HELMET)
    };
    public static final ItemStack[] ironArmour = new ItemStack[]{
            new ItemStack(ItemID.IRON_BOOTS), new ItemStack(ItemID.IRON_PANTS),
            new ItemStack(ItemID.IRON_CHEST), new ItemStack(ItemID.IRON_HELMET)
    };
    public static final ItemStack[] goldArmour = new ItemStack[]{
            new ItemStack(ItemID.GOLD_BOOTS), new ItemStack(ItemID.GOLD_PANTS),
            new ItemStack(ItemID.GOLD_CHEST), new ItemStack(ItemID.GOLD_HELMET)
    };
    public static final ItemStack[] diamondArmour = new ItemStack[]{
            new ItemStack(ItemID.DIAMOND_BOOTS), new ItemStack(ItemID.DIAMOND_PANTS),
            new ItemStack(ItemID.DIAMOND_CHEST), new ItemStack(ItemID.DIAMOND_HELMET)
    };

    public static class Ancient {

        public static ItemStack makeHelmet() {

            ItemStack ancientHelmet = new ItemStack(ItemID.CHAINMAIL_HELMET);
            ItemMeta ancientMeta = ancientHelmet.getItemMeta();
            ancientMeta.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 4, true);
            ancientMeta.addEnchant(Enchantment.PROTECTION_PROJECTILE, 4, true);
            ancientMeta.addEnchant(Enchantment.PROTECTION_FIRE, 4, true);
            ancientMeta.addEnchant(Enchantment.PROTECTION_EXPLOSIONS, 4, true);
            ancientMeta.addEnchant(Enchantment.OXYGEN, 3, true);
            ancientMeta.addEnchant(Enchantment.WATER_WORKER, 1, true);
            ancientMeta.setDisplayName(ChatColor.GOLD + "Ancient Helmet");
            ((Repairable) ancientMeta).setRepairCost(400);
            ancientHelmet.setItemMeta(ancientMeta);
            return ancientHelmet;
        }

        public static ItemStack makeChest() {

            ItemStack ancientChestplate = new ItemStack(ItemID.CHAINMAIL_CHEST);
            ItemMeta ancientMeta = ancientChestplate.getItemMeta();
            ancientMeta.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 4, true);
            ancientMeta.addEnchant(Enchantment.PROTECTION_PROJECTILE, 4, true);
            ancientMeta.addEnchant(Enchantment.PROTECTION_FIRE, 4, true);
            ancientMeta.addEnchant(Enchantment.PROTECTION_EXPLOSIONS, 4, true);
            ancientMeta.setDisplayName(ChatColor.GOLD + "Ancient Chestplate");
            ((Repairable) ancientMeta).setRepairCost(400);
            ancientChestplate.setItemMeta(ancientMeta);
            return ancientChestplate;
        }

        public static ItemStack makeLegs() {

            ItemStack ancientLeggings = new ItemStack(ItemID.CHAINMAIL_PANTS);
            ItemMeta ancientMeta = ancientLeggings.getItemMeta();
            ancientMeta.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 4, true);
            ancientMeta.addEnchant(Enchantment.PROTECTION_PROJECTILE, 4, true);
            ancientMeta.addEnchant(Enchantment.PROTECTION_FIRE, 4, true);
            ancientMeta.addEnchant(Enchantment.PROTECTION_EXPLOSIONS, 4, true);
            ancientMeta.setDisplayName(ChatColor.GOLD + "Ancient Leggings");
            ((Repairable) ancientMeta).setRepairCost(400);
            ancientLeggings.setItemMeta(ancientMeta);
            return ancientLeggings;
        }

        public static ItemStack makeBoots() {

            ItemStack ancientBoots = new ItemStack(ItemID.CHAINMAIL_BOOTS);
            ItemMeta ancientMeta = ancientBoots.getItemMeta();
            ancientMeta.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 4, true);
            ancientMeta.addEnchant(Enchantment.PROTECTION_PROJECTILE, 4, true);
            ancientMeta.addEnchant(Enchantment.PROTECTION_FIRE, 4, true);
            ancientMeta.addEnchant(Enchantment.PROTECTION_EXPLOSIONS, 4, true);
            ancientMeta.addEnchant(Enchantment.PROTECTION_FALL, 4, true);
            ancientMeta.setDisplayName(ChatColor.GOLD + "Ancient Boots");
            ((Repairable) ancientMeta).setRepairCost(400);
            ancientBoots.setItemMeta(ancientMeta);
            return ancientBoots;
        }
    }

    public static class God {

        public static ItemStack makeSword() {

            ItemStack godSword = new ItemStack(ItemID.DIAMOND_SWORD);
            ItemMeta godMeta = godSword.getItemMeta();
            godMeta.addEnchant(Enchantment.DAMAGE_ALL, 5, true);
            godMeta.addEnchant(Enchantment.DAMAGE_ARTHROPODS, 5, true);
            godMeta.addEnchant(Enchantment.DAMAGE_UNDEAD, 5, true);
            godMeta.addEnchant(Enchantment.FIRE_ASPECT, 2, true);
            godMeta.addEnchant(Enchantment.KNOCKBACK, 2, true);
            godMeta.addEnchant(Enchantment.LOOT_BONUS_MOBS, 3, true);
            godMeta.setDisplayName(ChatColor.RED + "God Sword");
            ((Repairable) godMeta).setRepairCost(400);
            godSword.setItemMeta(godMeta);
            return godSword;
        }

        public static ItemStack makeBow() {

            ItemStack godBow = new ItemStack(ItemID.BOW);
            ItemMeta godMeta = godBow.getItemMeta();
            godMeta.addEnchant(Enchantment.ARROW_DAMAGE, 5, true);
            godMeta.addEnchant(Enchantment.ARROW_FIRE, 1, true);
            godMeta.addEnchant(Enchantment.ARROW_INFINITE, 1, true);
            godMeta.addEnchant(Enchantment.ARROW_KNOCKBACK, 2, true);
            godMeta.setDisplayName(ChatColor.RED + "God Bow");
            ((Repairable) godMeta).setRepairCost(400);
            godBow.setItemMeta(godMeta);
            return godBow;
        }

        public static ItemStack makeHelmet() {

            ItemStack godHelmet = new ItemStack(ItemID.DIAMOND_HELMET);
            ItemMeta godMeta = godHelmet.getItemMeta();
            godMeta.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 4, true);
            godMeta.addEnchant(Enchantment.PROTECTION_PROJECTILE, 4, true);
            godMeta.addEnchant(Enchantment.PROTECTION_FIRE, 4, true);
            godMeta.addEnchant(Enchantment.PROTECTION_EXPLOSIONS, 4, true);
            godMeta.addEnchant(Enchantment.OXYGEN, 3, true);
            godMeta.addEnchant(Enchantment.WATER_WORKER, 1, true);
            godMeta.setDisplayName(ChatColor.BLUE + "God Helmet");
            ((Repairable) godMeta).setRepairCost(400);
            godHelmet.setItemMeta(godMeta);
            return godHelmet;
        }

        public static ItemStack makeChest() {

            ItemStack godChestplate = new ItemStack(ItemID.DIAMOND_CHEST);
            ItemMeta godMeta = godChestplate.getItemMeta();
            godMeta.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 4, true);
            godMeta.addEnchant(Enchantment.PROTECTION_PROJECTILE, 4, true);
            godMeta.addEnchant(Enchantment.PROTECTION_FIRE, 4, true);
            godMeta.addEnchant(Enchantment.PROTECTION_EXPLOSIONS, 4, true);
            godMeta.setDisplayName(ChatColor.BLUE + "God Chestplate");
            ((Repairable) godMeta).setRepairCost(400);
            godChestplate.setItemMeta(godMeta);
            return godChestplate;
        }

        public static ItemStack makeLegs() {

            ItemStack godLeggings = new ItemStack(ItemID.DIAMOND_PANTS);
            ItemMeta godMeta = godLeggings.getItemMeta();
            godMeta.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 4, true);
            godMeta.addEnchant(Enchantment.PROTECTION_PROJECTILE, 4, true);
            godMeta.addEnchant(Enchantment.PROTECTION_FIRE, 4, true);
            godMeta.addEnchant(Enchantment.PROTECTION_EXPLOSIONS, 4, true);
            godMeta.setDisplayName(ChatColor.BLUE + "God Leggings");
            ((Repairable) godMeta).setRepairCost(400);
            godLeggings.setItemMeta(godMeta);
            return godLeggings;
        }

        public static ItemStack makeBoots() {

            ItemStack godBoots = new ItemStack(ItemID.DIAMOND_BOOTS);
            ItemMeta godMeta = godBoots.getItemMeta();
            godMeta.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 4, true);
            godMeta.addEnchant(Enchantment.PROTECTION_PROJECTILE, 4, true);
            godMeta.addEnchant(Enchantment.PROTECTION_FIRE, 4, true);
            godMeta.addEnchant(Enchantment.PROTECTION_EXPLOSIONS, 4, true);
            godMeta.addEnchant(Enchantment.PROTECTION_FALL, 4, true);
            godMeta.setDisplayName(ChatColor.BLUE + "God Boots");
            ((Repairable) godMeta).setRepairCost(400);
            godBoots.setItemMeta(godMeta);
            return godBoots;
        }

        public static ItemStack makePickaxe(boolean legendary) {

            ItemStack pickaxe = new ItemStack(ItemID.DIAMOND_PICKAXE);
            if (legendary) {
                pickaxe.addEnchantment(Enchantment.DIG_SPEED, 5);
                pickaxe.addEnchantment(Enchantment.DURABILITY, 3);
                pickaxe.addEnchantment(Enchantment.LOOT_BONUS_BLOCKS, 3);
                ItemMeta godMeta = pickaxe.getItemMeta();
                godMeta.setDisplayName(ChatColor.GREEN + "Legendary God Pickaxe");
                pickaxe.setItemMeta(godMeta);
            } else {
                pickaxe.addEnchantment(Enchantment.DIG_SPEED, 4);
                pickaxe.addEnchantment(Enchantment.SILK_TOUCH, 1);
                ItemMeta godMeta = pickaxe.getItemMeta();
                godMeta.setDisplayName(ChatColor.GREEN + "God Pickaxe");
                pickaxe.setItemMeta(godMeta);
            }
            return pickaxe;
        }
    }

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

    public static class CPotion {

        public static ItemStack divineCombatPotion() {

            int time = 20 * 600;
            int level = 5;

            ItemStack divineCombatPotion = new Potion(PotionType.INSTANT_DAMAGE).toItemStack(1);
            PotionMeta pMeta = (PotionMeta) divineCombatPotion.getItemMeta();
            pMeta.addCustomEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, time, level), false);
            pMeta.addCustomEffect(new PotionEffect(PotionEffectType.REGENERATION, time, level), false);
            pMeta.addCustomEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, time, level), false);
            pMeta.addCustomEffect(new PotionEffect(PotionEffectType.WATER_BREATHING, time, level), false);
            pMeta.addCustomEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, time, level), false);
            pMeta.setDisplayName(ChatColor.WHITE + "Divine Combat Potion");
            divineCombatPotion.setItemMeta(pMeta);
            return divineCombatPotion;
        }

        public static ItemStack holyCombatPotion() {

            int time = 20 * 45;
            int level = 5;

            ItemStack holyCombatPotion = new Potion(PotionType.INSTANT_DAMAGE).toItemStack(1);
            PotionMeta pMeta = (PotionMeta) holyCombatPotion.getItemMeta();
            pMeta.addCustomEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, time, level), false);
            pMeta.addCustomEffect(new PotionEffect(PotionEffectType.REGENERATION, time, level), false);
            pMeta.addCustomEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, time, level), false);
            pMeta.addCustomEffect(new PotionEffect(PotionEffectType.WATER_BREATHING, time, level), false);
            pMeta.addCustomEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, time, level), false);
            pMeta.setDisplayName(ChatColor.WHITE + "Holy Combat Potion");
            holyCombatPotion.setItemMeta(pMeta);
            return holyCombatPotion;
        }

        public static ItemStack extremeCombatPotion() {

            int time = 20 * 600;
            int level = 2;

            ItemStack extremeCombatPotion = new Potion(PotionType.INSTANT_DAMAGE).toItemStack(1);
            PotionMeta pMeta = (PotionMeta) extremeCombatPotion.getItemMeta();
            pMeta.addCustomEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, time, level), false);
            pMeta.addCustomEffect(new PotionEffect(PotionEffectType.REGENERATION, time, level), false);
            pMeta.addCustomEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, time, level), false);
            pMeta.addCustomEffect(new PotionEffect(PotionEffectType.WATER_BREATHING, time, level), false);
            pMeta.addCustomEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, time, level), false);
            pMeta.setDisplayName(ChatColor.WHITE + "Extreme Combat Potion");
            extremeCombatPotion.setItemMeta(pMeta);
            return extremeCombatPotion;
        }
    }

    public static class Misc {

        public static ItemStack overseerBow() {

            ItemStack overseerBow = new ItemStack(ItemID.BOW);
            ItemMeta overseerMeta = overseerBow.getItemMeta();
            overseerMeta.addEnchant(Enchantment.ARROW_DAMAGE, 2, true);
            overseerMeta.addEnchant(Enchantment.ARROW_FIRE, 1, true);
            overseerMeta.setDisplayName(ChatColor.RED + "Overseer's Bow");
            ((Repairable) overseerMeta).setRepairCost(400);
            overseerBow.setItemMeta(overseerMeta);
            return overseerBow;
        }

        public static ItemStack godFish(int amount) {

            ItemStack godFish = new ItemStack(ItemID.RAW_FISH, amount);
            ItemMeta fishMeta = godFish.getItemMeta();
            fishMeta.setDisplayName(ChatColor.BLUE + "God Fish");
            godFish.setItemMeta(fishMeta);
            return godFish;
        }

        public static ItemStack gemOfDarkness(int amount) {

            ItemStack gemOfDarkness = new ItemStack(ItemID.EMERALD, amount);
            ItemMeta gemMeta = gemOfDarkness.getItemMeta();
            gemMeta.setDisplayName(ChatColor.DARK_RED + "Gem of Darkness");
            gemOfDarkness.setItemMeta(gemMeta);
            return gemOfDarkness;
        }

        public static ItemStack pixieDust(int amount) {

            ItemStack phantomGold = new ItemStack(ItemID.SUGAR, amount);
            ItemMeta goldMeta = phantomGold.getItemMeta();
            goldMeta.setDisplayName(ChatColor.GOLD + "Pixie Dust");
            phantomGold.setItemMeta(goldMeta);
            return phantomGold;
        }

        public static ItemStack phantomGold(int amount) {

            ItemStack phantomGold = new ItemStack(ItemID.GOLD_BAR, amount);
            ItemMeta goldMeta = phantomGold.getItemMeta();
            goldMeta.setDisplayName(ChatColor.GOLD + "Phantom Gold");
            phantomGold.setItemMeta(goldMeta);
            return phantomGold;
        }

        public static ItemStack phantomClock(int amount) {

            ItemStack phantomClock = new ItemStack(ItemID.WATCH, amount);
            ItemMeta phantomMeta = phantomClock.getItemMeta();
            phantomMeta.setDisplayName(ChatColor.DARK_RED + "Phantom Clock");
            phantomClock.setItemMeta(phantomMeta);
            return phantomClock;
        }

        public static ItemStack imbuedCrystal(int amount) {

            ItemStack gemOfDarkness = new ItemStack(ItemID.DIAMOND, amount);
            ItemMeta gemMeta = gemOfDarkness.getItemMeta();
            gemMeta.setDisplayName(ChatColor.AQUA + "Imbued Crystal");
            gemOfDarkness.setItemMeta(gemMeta);
            return gemOfDarkness;
        }

        public static ItemStack gemOfLife(int amount) {

            ItemStack gemOfLife = new ItemStack(ItemID.DIAMOND, amount);
            ItemMeta gemMeta = gemOfLife.getItemMeta();
            gemMeta.setDisplayName(ChatColor.DARK_AQUA + "Gem of Life");
            gemOfLife.setItemMeta(gemMeta);
            return gemOfLife;
        }

        public static ItemStack barbarianBone(int amount) {

            ItemStack barbarianBones = new ItemStack(ItemID.BONE, amount);
            ItemMeta boneMeta = barbarianBones.getItemMeta();
            boneMeta.setDisplayName(ChatColor.DARK_RED + "Barbarian Bone");
            barbarianBones.setItemMeta(boneMeta);
            return barbarianBones;
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

        public static ItemStack magicBucket() {

            ItemStack magicBucket = new ItemStack(ItemID.BUCKET);
            ItemMeta magicMeta = magicBucket.getItemMeta();
            magicMeta.setDisplayName(ChatColor.DARK_PURPLE + "Magic Bucket");
            magicBucket.setItemMeta(magicMeta);
            return magicBucket;
        }

        public static ItemStack cursedGold(int amount, boolean ore) {

            ItemStack cursedGold = new ItemStack(ore ? BlockID.GOLD_ORE : ItemID.GOLD_BAR, amount);
            ItemMeta goldMeta = cursedGold.getItemMeta();
            goldMeta.setDisplayName(ChatColor.GOLD + "Cursed Gold" + (ore ? "" : " Bar"));
            cursedGold.setItemMeta(goldMeta);
            return cursedGold;
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

    public static boolean removeItemOfType(InventoryHolder inventoryHolder, int typeId, int quantity, boolean ignoreToSmall) {

        int c = ItemUtil.countItemsOfType(inventoryHolder.getInventory().getContents(), typeId);

        if (c == 0 || (c < quantity && !ignoreToSmall)) return false;

        c -= Math.min(c, quantity);

        ItemStack[] stacks = ItemUtil.removeItemOfType(inventoryHolder.getInventory().getContents(), typeId);

        inventoryHolder.getInventory().setContents(stacks);

        int amount = Math.min(c, Material.getMaterial(typeId).getMaxStackSize());
        while (amount > 0) {
            inventoryHolder.getInventory().addItem(new ItemStack(typeId, amount));
            c -= amount;
            amount = Math.min(c, 64);
        }

        if (inventoryHolder instanceof Player) {
            //noinspection deprecation
            ((Player) inventoryHolder).updateInventory();
        }
        return true;
    }

    public static ItemStack[] removeItemOfType(ItemStack[] itemStacks, int typeId) {

        for (int i = 0; i < itemStacks.length; i++) {
            ItemStack is = itemStacks[i];
            if (is != null && !isNamed(is) && is.getTypeId() == typeId) {
                itemStacks[i] = null;
            }
        }
        return itemStacks;
    }

    public static ItemStack[] removeItemOfName(ItemStack[] itemStacks, String name) {

        for (int i = 0; i < itemStacks.length; i++) {
            if (matchesFilter(itemStacks[i], name, false)) {
                itemStacks[i] = null;
            }
        }
        return itemStacks;
    }

    public static boolean removeItemOfName(InventoryHolder inventoryHolder, ItemStack stack, int quantity, boolean ignoreToSmall) {

        // Force to 1
        stack.setAmount(1);

        // Get name
        String itemName = stack.getItemMeta().getDisplayName();

        int c = ItemUtil.countItemsOfName(inventoryHolder.getInventory().getContents(), itemName);

        if (c == 0 || (c < quantity && !ignoreToSmall)) return false;

        c -= Math.min(c, quantity);

        ItemStack[] stacks = ItemUtil.removeItemOfName(inventoryHolder.getInventory().getContents(), itemName);

        inventoryHolder.getInventory().setContents(stacks);

        int amount = Math.min(c, Material.getMaterial(stack.getTypeId()).getMaxStackSize());
        while (amount > 0) {
            // Clone the item and add it
            ItemStack aStack = stack.clone();
            aStack.setAmount(amount);
            inventoryHolder.getInventory().addItem(aStack);
            c -= amount;
            amount = Math.min(c, 64);
        }

        if (inventoryHolder instanceof Player) {
            //noinspection deprecation
            ((Player) inventoryHolder).updateInventory();
        }
        return true;
    }

    public static int countItemsOfName(ItemStack[] itemStacks, String name) {

        int count = 0;
        for (ItemStack itemStack : itemStacks) {
            if (matchesFilter(itemStack, name, false)) count += itemStack.getAmount();
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

    private static final int[] axes = new int[]{
            ItemID.WOOD_AXE, ItemID.STONE_AXE,
            ItemID.IRON_AXE, ItemID.GOLD_AXE,
            ItemID.DIAMOND_AXE
    };

    public static boolean isAxe(int itemId) {

        for (int axe : axes) {
            if (itemId == axe) {
                return true;
            }
        }
        return false;
    }

    private static final int[] pickAxes = new int[]{
            ItemID.WOOD_PICKAXE, ItemID.STONE_PICKAXE,
            ItemID.IRON_PICK, ItemID.GOLD_PICKAXE,
            ItemID.DIAMOND_PICKAXE
    };

    public static boolean isPickAxe(int itemId) {

        for (int pickAxe : pickAxes) {
            if (itemId == pickAxe) {
                return true;
            }
        }
        return false;
    }

    public static boolean isTool(int itemId) {

        return isAxe(itemId) || isPickAxe(itemId);
    }

    private static final int[] ingots = new int[]{
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

    public static boolean hasAncientArmour(LivingEntity entity) {

        if (!entity.isValid()) return false;

        ItemStack[] armour;
        EntityEquipment equipment = entity.getEquipment();
        if (equipment != null) armour = equipment.getArmorContents();
        else return false;

        boolean[] b = new boolean[]{false, false, false, false};

        for (int i = 0; i < 4; i++) {
            b[i] = matchesFilter(armour[i], ChatColor.GOLD + "Ancient");
        }
        return b[0] && b[1] && b[2] && b[3];
    }

    public static boolean isNamed(ItemStack stack) {

        return stack != null && stack.hasItemMeta() && stack.getItemMeta().hasDisplayName();
    }

    public static boolean isAuthenticCustomItem(String itemName) {

        return !(itemName.indexOf(ChatColor.COLOR_CHAR) == -1);
    }

    public static boolean matchesFilter(ItemStack stack, String filter) {

        return matchesFilter(stack, filter, true);
    }

    public static boolean matchesFilter(ItemStack stack, String filter, boolean loose) {

        return isNamed(stack) && (loose ? stack.getItemMeta().getDisplayName().startsWith(filter) : stack.getItemMeta().getDisplayName().equals(filter));
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
        return pickaxe.getEnchantmentLevel(Enchantment.LOOT_BONUS_BLOCKS) + 1;
    }
}
