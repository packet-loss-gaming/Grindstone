/*
 * Copyright (c) 2014 Wyatt Childers.
 *
 * All Rights Reserved
 */

package com.skelril.aurora.util.item;

import com.sk89q.worldedit.blocks.BlockID;
import com.sk89q.worldedit.blocks.ItemID;
import com.skelril.aurora.util.ChanceUtil;
import com.skelril.aurora.util.item.itemstack.SerializableItemStack;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.*;
import org.bukkit.potion.Potion;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;

import java.util.*;

import static com.skelril.aurora.util.item.ItemUtil.CustomItems.*;

/**
 * Author: Turtle9598
 */
public class ItemUtil {

    private static List<String> sacSrcs = Arrays.asList(
            "Enchanted Forest",
            "Drop Parties",
            "Vineam Prison",
            "Giant Boss",
            "Wilderness Mobs",
            "Sacrificial Pit"
    );

    public static enum CustomItems {

        // Ancient Armor
        ANCIENT_CROWN(ChatColor.GOLD, "Ancient Crown",
                Arrays.asList("Double Health Regen",
                        "Double XP Gain",
                        "Acts as an Imbued Crystal",
                        "Acts as a Gem of Darkness",
                        "Acts as an Ancient Helmet"),
                Arrays.asList("Giant Boss",
                        "Market")
        ),
        ANCIENT_HELMET(ChatColor.GOLD, "Ancient Helmet",
                Arrays.asList("Set Effect: Ancient Armor"),
                Arrays.asList("Grave Yard",
                        "Market"),
                true
        ),
        ANCIENT_CHESTPLATE(ChatColor.GOLD, "Ancient Chestplate",
                Arrays.asList("Set Effect: Ancient Armor"),
                Arrays.asList("Grave Yard",
                        "Market"),
                true
        ),
        ANCIENT_LEGGINGS(ChatColor.GOLD, "Ancient Leggings",
                Arrays.asList("Set Effect: Ancient Armor"),
                Arrays.asList("Grave Yard",
                        "Market"),
                true
        ),
        ANCIENT_BOOTS(ChatColor.GOLD, "Ancient Boots",
                Arrays.asList("Set Effect: Ancient Armor"),
                Arrays.asList("Grave Yard",
                        "Market"),
                true
        ),

        // Master Weapons
        MASTER_SWORD(ChatColor.DARK_PURPLE, "Master Sword", 2,
                Arrays.asList("Repairable at any Sacrificial Pit",
                        "Conditional Effects"),
                Arrays.asList("Giant Boss",
                        "Market")
        ),
        MASTER_BOW(ChatColor.DARK_PURPLE, "Master Bow", 2,
                Arrays.asList("Repairable at any Sacrificial Pit",
                        "Conditional Effects"),
                Arrays.asList("Giant Boss",
                        "Market")
        ),

        // Unleashed Weapons
        UNLEASHED_SWORD(ChatColor.DARK_PURPLE, "Unleashed Sword", 2.25,
                Arrays.asList("Repairable at any Sacrificial Pit, " +
                                "but requires 2 Imbued Crystals for " +
                                "every 11% damage, or 1 Imbued Crystal " +
                                "if repaired inside of the Grave " +
                                "Yard rewards room.",
                        "Global Effects"),
                Arrays.asList("Grave Yard",
                        "Market")
        ),
        UNLEASHED_BOW(ChatColor.DARK_PURPLE, "Unleashed Bow", 2.25,
                Arrays.asList("Repairable at any Sacrificial Pit, " +
                                "but requires 2 Imbued Crystals for " +
                                "every 11% damage, or 1 Imbued Crystal " +
                                "if repaired inside of the Grave " +
                                "Yard rewards room.",
                        "Global Effects"),
                Arrays.asList("Grave Yard",
                        "Market")
        ),

        // Fear Weapons
        FEAR_SWORD(ChatColor.DARK_RED, "Fear Sword", 2.25,
                Arrays.asList("Repairable at any Sacrificial Pit, " +
                                "but requires 2 Gems of Darkness for " +
                                "every 11% damage, or 1 Gem of Darkness " +
                                "if repaired inside of the Grave " +
                                "Yard rewards room.",
                        "Global Effects"),
                Arrays.asList("Grave Yard",
                        "Market")
        ),
        FEAR_BOW(ChatColor.DARK_RED, "Fear Bow", 2.25,
                Arrays.asList("Repairable at any Sacrificial Pit, " +
                                "but requires 2 Gems of Darkness for " +
                                "every 11% damage, or 1 Gem of Darkness " +
                                "if repaired inside of the Grave " +
                                "Yard rewards room.",
                        "Global Effects"),
                Arrays.asList("Grave Yard",
                        "Market")
        ),

        // Red Items
        RED_FEATHER(ChatColor.DARK_RED, "Red Feather",
                Arrays.asList("Consumes redstone to prevent up to 100% damage, " +
                        "but has a cool down based on the amount of damage taken"),
                Arrays.asList("Wilderness Mobs",
                        "Market")
        ),

        // God Weapons
        GOD_SWORD(ChatColor.RED, "God Sword", 1.5,
                null,
                Arrays.asList("Grave Yard",
                        "Market"),
                true
        ),
        GOD_BOW(ChatColor.RED, "God Bow", 1.5,
                null,
                Arrays.asList("Grave Yard",
                        "Market"),
                true
        ),

        // God Tools
        GOD_AXE(ChatColor.GREEN, "God Axe",
                null,
                Arrays.asList("Grave Yard",
                        "Market"),
                true
        ),
        LEGENDARY_GOD_AXE(ChatColor.GREEN, "Legendary God Axe",
                null,
                Arrays.asList("Grave Yard",
                        "Market"),
                true
        ),
        GOD_PICKAXE(ChatColor.GREEN, "God Pickaxe",
                null,
                Arrays.asList("Grave Yard",
                        "Market"),
                true
        ),
        LEGENDARY_GOD_PICKAXE(ChatColor.GREEN, "Legendary God Pickaxe",
                null,
                Arrays.asList("Grave Yard",
                        "Market"),
                true
        ),

        // God Armor
        GOD_HELMET(ChatColor.BLUE, "God Helmet", null,
                Arrays.asList("Grave Yard",
                        "Market"),
                true
        ),
        GOD_CHESTPLATE(ChatColor.BLUE, "God Chestplate", null,
                Arrays.asList("Grave Yard",
                        "Market"),
                true
        ),
        GOD_LEGGINGS(ChatColor.BLUE, "God Leggings", null,
                Arrays.asList("Grave Yard",
                        "Market"),
                true
        ),
        GOD_BOOTS(ChatColor.BLUE, "God Boots",
                null,
                Arrays.asList("Grave Yard",
                        "Market"),
                true
        ),

        // Combat Potions
        DIVINE_COMBAT_POTION(ChatColor.WHITE, "Divine Combat Potion",
                null,
                Arrays.asList("Grave Yard",
                        "Market"),
                true
        ),
        HOLY_COMBAT_POTION(ChatColor.WHITE, "Holy Combat Potion",
                null,
                Arrays.asList("Grave Yard",
                        "Market"),
                true
        ),
        EXTREME_COMBAT_POTION(ChatColor.WHITE, "Extreme Combat Potion",
                null,
                Arrays.asList("Grave Yard",
                        "Market"),
                true
        ),

        // Grave Yard Gems
        GEM_OF_LIFE(ChatColor.DARK_AQUA, "Gem of Life",
                null,
                Arrays.asList("Grave Yard",
                        "Market")
        ),
        GEM_OF_DARKNESS(ChatColor.DARK_RED, "Gem of Darkness",
                null,
                Arrays.asList("Grave Yard",
                        "Market")
        ),
        IMBUED_CRYSTAL(ChatColor.AQUA, "Imbued Crystal",
                null,
                Arrays.asList("Grave Yard",
                        "Market")
        ),

        // Phantom Items
        PHANTOM_GOLD(ChatColor.GOLD, "Phantom Gold",
                null,
                Arrays.asList("Gold Rush",
                        "Grave Yard")
        ),
        PHANTOM_CLOCK(ChatColor.DARK_RED, "Phantom Clock",
                null,
                Arrays.asList("Grave Yard")
        ),
        PHANTOM_HYMN(ChatColor.DARK_RED, "Phantom Hymn",
                null,
                Arrays.asList("Gold Rush",
                        "Market")
        ),

        // Ninja Guild
        NINJA_STAR(ChatColor.BLACK, "Ninja Star",
                null,
                Arrays.asList("Ninja Guild")
        ),

        // Flight Items
        PIXIE_DUST(ChatColor.GOLD, "Pixie Dust",
                null,
                Arrays.asList("Gold Rush",
                        "Market"),
                true
        ),
        MAGIC_BUCKET(ChatColor.DARK_PURPLE, "Magic Bucket",
                null,
                Arrays.asList("Giant Boss",
                        "Market")
        ),

        // Animal Bow
        BAT_BOW(ChatColor.DARK_RED, "Bat Bow",
                null,
                Arrays.asList("Grave Yard",
                        "Market")
        ),
        CHICKEN_BOW(ChatColor.GOLD, "Chicken Bow"),

        // Animal Hymn
        CHICKEN_HYMN(ChatColor.GOLD, "Chicken Hymn"),


        // Miscellaneous
        GOD_FISH(ChatColor.BLUE, "God Fish",
                null,
                Arrays.asList("Arrow Fishing",
                        "Market"),
                true
        ),
        OVERSEER_BOW(ChatColor.RED, "Overseer's Bow",
                null,
                Arrays.asList("Market"),
                true
        ),
        BARBARIAN_BONE(ChatColor.DARK_RED, "Barbarian Bone",
                null,
                Arrays.asList("Grave Yard")
        ),
        POTION_OF_RESTITUTION(ChatColor.DARK_RED, "Potion of Restitution",
                null,
                Arrays.asList("Wilderness Mobs"));

        private ChatColor color;
        private String name;
        private double dmgMod;
        private List<String> uses;
        private List<String> srcs;

        private CustomItems(ChatColor color, String name) {
            this(color, name, null, null);
        }

        private CustomItems(ChatColor color, String name, List<String> uses, List<String> srcs) {
            this(color, name, -1, uses, srcs);
        }

        private CustomItems(ChatColor color, String name, double dmgMod, List<String> uses, List<String> srcs) {
            this(color, name, dmgMod, uses, srcs, false);
        }

        private CustomItems(ChatColor color, String name, List<String> uses, List<String> srcs, boolean sacPit) {
            this(color, name, -1, uses, srcs, sacPit);
        }

        private CustomItems(ChatColor color, String name, double dmgMod, List<String> uses, List<String> srcs, boolean sacPit) {
            this.color = color;
            this.name = name;
            this.dmgMod = dmgMod;

            // Uses Handling
            this.uses = new ArrayList<>();
            if (uses != null) {
                this.uses.addAll(uses);
            }
            if (dmgMod != -1) {
                this.uses.add("Provides a damage modifier of: " + dmgMod);
            }
            if (this.uses.size() < 1) {
                this.uses = null;
            }

            // Sources Handling
            this.srcs = new ArrayList<>();
            if (sacPit) {
                this.srcs.addAll(sacSrcs);
            }
            if (srcs != null) {
                this.srcs.addAll(srcs);
            }
            if (this.srcs.size() < 1) {
                this.srcs = null;
            }
        }

        public ChatColor getColor() {
            return color;
        }

        public String getName() {
            return name;
        }

        public double getDmgMod() {
            return dmgMod;
        }

        public List<String> getUses() {
            return uses;
        }

        public List<String> getSrcs() {
            return srcs;
        }

        @Override
        public String toString() {
            return color + name;
        }
    }

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

        public static ItemStack makeCrown() {

            ItemStack ancientHelmet = new ItemStack(ItemID.GOLD_HELMET);
            ItemMeta ancientMeta = ancientHelmet.getItemMeta();
            ancientMeta.addEnchant(Enchantment.DURABILITY, 3, true);
            ancientMeta.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 4, true);
            ancientMeta.addEnchant(Enchantment.PROTECTION_PROJECTILE, 4, true);
            ancientMeta.addEnchant(Enchantment.PROTECTION_FIRE, 4, true);
            ancientMeta.addEnchant(Enchantment.PROTECTION_EXPLOSIONS, 4, true);
            ancientMeta.addEnchant(Enchantment.OXYGEN, 3, true);
            ancientMeta.addEnchant(Enchantment.WATER_WORKER, 1, true);
            ancientMeta.setDisplayName(ANCIENT_CROWN.toString());
            ((Repairable) ancientMeta).setRepairCost(400);
            ancientHelmet.setItemMeta(ancientMeta);
            return ancientHelmet;
        }

        public static ItemStack makeHelmet() {

            ItemStack ancientHelmet = new ItemStack(ItemID.CHAINMAIL_HELMET);
            ItemMeta ancientMeta = ancientHelmet.getItemMeta();
            ancientMeta.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 4, true);
            ancientMeta.addEnchant(Enchantment.PROTECTION_PROJECTILE, 4, true);
            ancientMeta.addEnchant(Enchantment.PROTECTION_FIRE, 4, true);
            ancientMeta.addEnchant(Enchantment.PROTECTION_EXPLOSIONS, 4, true);
            ancientMeta.addEnchant(Enchantment.OXYGEN, 3, true);
            ancientMeta.addEnchant(Enchantment.WATER_WORKER, 1, true);
            ancientMeta.setDisplayName(ANCIENT_HELMET.toString());
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
            ancientMeta.setDisplayName(ANCIENT_CHESTPLATE.toString());
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
            ancientMeta.setDisplayName(ANCIENT_LEGGINGS.toString());
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
            ancientMeta.setDisplayName(ANCIENT_BOOTS.toString());
            ((Repairable) ancientMeta).setRepairCost(400);
            ancientBoots.setItemMeta(ancientMeta);
            return ancientBoots;
        }
    }

    public static class God {

        public static ItemStack makeSword() {

            ItemStack godSword = new ItemStack(ItemID.DIAMOND_SWORD);
            ItemMeta godMeta = godSword.getItemMeta();
            godMeta.setLore(Arrays.asList(ChatColor.RED + "Damage Modifier: " + GOD_SWORD.getDmgMod()));
            godMeta.setDisplayName(GOD_SWORD.toString());
            ((Repairable) godMeta).setRepairCost(400);
            godSword.setItemMeta(godMeta);
            return godSword;
        }

        public static ItemStack makeBow() {

            ItemStack godBow = new ItemStack(ItemID.BOW);
            ItemMeta godMeta = godBow.getItemMeta();
            godMeta.setLore(Arrays.asList(ChatColor.RED + "Damage Modifier: " + GOD_BOW.getDmgMod()));
            godMeta.setDisplayName(GOD_BOW.toString());
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
            godMeta.setDisplayName(GOD_HELMET.toString());
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
            godMeta.setDisplayName(GOD_CHESTPLATE.toString());
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
            godMeta.setDisplayName(GOD_LEGGINGS.toString());
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
            godMeta.setDisplayName(GOD_BOOTS.toString());
            ((Repairable) godMeta).setRepairCost(400);
            godBoots.setItemMeta(godMeta);
            return godBoots;
        }

        public static ItemStack makePickaxe(boolean legendary) {

            ItemStack pickaxe = new ItemStack(ItemID.DIAMOND_PICKAXE);
            ItemMeta godMeta = pickaxe.getItemMeta();

            if (legendary) {
                godMeta.addEnchant(Enchantment.DIG_SPEED, 5, true);
                godMeta.addEnchant(Enchantment.DURABILITY, 3, true);
                godMeta.addEnchant(Enchantment.LOOT_BONUS_BLOCKS, 3, true);
                godMeta.setDisplayName(LEGENDARY_GOD_PICKAXE.toString());
            } else {
                godMeta.addEnchant(Enchantment.DIG_SPEED, 4, true);
                godMeta.addEnchant(Enchantment.SILK_TOUCH, 1, true);
                godMeta.setDisplayName(GOD_PICKAXE.toString());
            }

            pickaxe.setItemMeta(godMeta);

            return pickaxe;
        }

        public static ItemStack makeAxe(boolean legendary) {

            ItemStack axe = new ItemStack(ItemID.DIAMOND_AXE);
            ItemMeta godMeta = axe.getItemMeta();

            if (legendary) {
                godMeta.addEnchant(Enchantment.DAMAGE_ALL, 5, true);
                godMeta.addEnchant(Enchantment.DAMAGE_ARTHROPODS, 5, true);
                godMeta.addEnchant(Enchantment.DAMAGE_UNDEAD, 5, true);
                godMeta.addEnchant(Enchantment.DIG_SPEED, 5, true);
                godMeta.addEnchant(Enchantment.DURABILITY, 3, true);
                godMeta.setDisplayName(LEGENDARY_GOD_AXE.toString());
            } else {
                godMeta.addEnchant(Enchantment.DIG_SPEED, 4, true);
                godMeta.setDisplayName(GOD_AXE.toString());
            }

            axe.setItemMeta(godMeta);

            return axe;
        }
    }

    public static class Master {

        public static ItemStack makeBow() {

            ItemStack masterBow = new ItemStack(ItemID.BOW);
            ItemMeta masterMeta = masterBow.getItemMeta();
            masterMeta.setLore(Arrays.asList(ChatColor.RED + "Damage Modifier: " + MASTER_BOW.getDmgMod()));
            masterMeta.setDisplayName(MASTER_BOW.toString());
            ((Repairable) masterMeta).setRepairCost(400);
            masterBow.setItemMeta(masterMeta);
            return masterBow;
        }

        public static ItemStack makeSword() {

            ItemStack masterSword = new ItemStack(ItemID.DIAMOND_SWORD);
            ItemMeta masterMeta = masterSword.getItemMeta();
            masterMeta.setLore(Arrays.asList(ChatColor.RED + "Damage Modifier: " + MASTER_SWORD.getDmgMod()));
            masterMeta.setDisplayName(MASTER_SWORD.toString());
            ((Repairable) masterMeta).setRepairCost(400);
            masterSword.setItemMeta(masterMeta);
            return masterSword;
        }
    }

    public static class Unleashed {

        public static ItemStack makeSword() {

            ItemStack unleashedSword = Master.makeSword();
            ItemMeta unleashedMeta = unleashedSword.getItemMeta();
            unleashedMeta.setLore(Arrays.asList(ChatColor.RED + "Damage Modifier: " + UNLEASHED_SWORD.getDmgMod()));
            unleashedMeta.setDisplayName(UNLEASHED_SWORD.toString());
            unleashedSword.setItemMeta(unleashedMeta);
            return unleashedSword;
        }

        public static ItemStack makeBow() {

            ItemStack unleashedBow = Master.makeBow();
            ItemMeta unleashedMeta = unleashedBow.getItemMeta();
            unleashedMeta.setLore(Arrays.asList(ChatColor.RED + "Damage Modifier: " + UNLEASHED_BOW.getDmgMod()));
            unleashedMeta.setDisplayName(UNLEASHED_BOW.toString());
            unleashedBow.setItemMeta(unleashedMeta);
            return unleashedBow;
        }
    }

    public static class Fear {

        public static ItemStack makeSword() {

            ItemStack fearSword = new ItemStack(ItemID.DIAMOND_SWORD);
            ItemMeta fearMeta = fearSword.getItemMeta();
            fearMeta.setLore(Arrays.asList(ChatColor.RED + "Damage Modifier: " + FEAR_SWORD.getDmgMod()));
            fearMeta.setDisplayName(FEAR_SWORD.toString());
            ((Repairable) fearMeta).setRepairCost(400);
            fearSword.setItemMeta(fearMeta);
            return fearSword;
        }

        public static ItemStack makeBow() {

            ItemStack fearBow = new ItemStack(ItemID.BOW);
            ItemMeta fearMeta = fearBow.getItemMeta();
            fearMeta.setLore(Arrays.asList(ChatColor.RED + "Damage Modifier: " + FEAR_BOW.getDmgMod()));
            fearMeta.setDisplayName(FEAR_BOW.toString());
            ((Repairable) fearMeta).setRepairCost(400);
            fearBow.setItemMeta(fearMeta);
            return fearBow;
        }
    }

    public static class Red {

        public static ItemStack makeFeather() {

            ItemStack redFeather = new ItemStack(ItemID.FEATHER);
            ItemMeta redMeta = redFeather.getItemMeta();
            redMeta.setDisplayName(RED_FEATHER.toString());
            redFeather.setItemMeta(redMeta);
            return redFeather;
        }
    }

    public static class CPotion {

        public static ItemStack divineCombatPotion() {

            int time = 20 * 600;
            int level = 3;

            ItemStack divineCombatPotion = new Potion(PotionType.INSTANT_DAMAGE).toItemStack(1);
            PotionMeta pMeta = (PotionMeta) divineCombatPotion.getItemMeta();
            pMeta.addCustomEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, time, level), false);
            pMeta.addCustomEffect(new PotionEffect(PotionEffectType.REGENERATION, time, level), false);
            pMeta.addCustomEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, time, level), false);
            pMeta.addCustomEffect(new PotionEffect(PotionEffectType.WATER_BREATHING, time, level), false);
            pMeta.addCustomEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, time, level), false);
            pMeta.setDisplayName(DIVINE_COMBAT_POTION.toString());
            divineCombatPotion.setItemMeta(pMeta);
            return divineCombatPotion;
        }

        public static ItemStack holyCombatPotion() {

            int time = 20 * 45;
            int level = 3;

            ItemStack holyCombatPotion = new Potion(PotionType.INSTANT_DAMAGE).toItemStack(1);
            PotionMeta pMeta = (PotionMeta) holyCombatPotion.getItemMeta();
            pMeta.addCustomEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, time, level), false);
            pMeta.addCustomEffect(new PotionEffect(PotionEffectType.REGENERATION, time, level), false);
            pMeta.addCustomEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, time, level), false);
            pMeta.addCustomEffect(new PotionEffect(PotionEffectType.WATER_BREATHING, time, level), false);
            pMeta.addCustomEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, time, level), false);
            pMeta.setDisplayName(HOLY_COMBAT_POTION.toString());
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
            pMeta.setDisplayName(EXTREME_COMBAT_POTION.toString());
            extremeCombatPotion.setItemMeta(pMeta);
            return extremeCombatPotion;
        }
    }

    public static class MPotion {

        public static ItemStack potionOfRestitution() {

            ItemStack restitutionPotion = new Potion(PotionType.POISON).toItemStack(1);
            PotionMeta pMeta = (PotionMeta) restitutionPotion.getItemMeta();
            pMeta.setDisplayName(POTION_OF_RESTITUTION.toString());
            pMeta.addCustomEffect(new PotionEffect(PotionEffectType.POISON, 20 * 10, 1), true);
            restitutionPotion.setItemMeta(pMeta);
            return restitutionPotion;
        }
    }

    public static class Guild {

        public static class Ninja {

            public static ItemStack makeStar(int amount) {

                ItemStack ninjaStar = new ItemStack(ItemID.NETHER_STAR, amount);
                ItemMeta starMeta = ninjaStar.getItemMeta();
                starMeta.setDisplayName(NINJA_STAR.toString());
                ninjaStar.setItemMeta(starMeta);
                return ninjaStar;
            }
        }
    }

    public static class Misc {

        public static ItemStack overseerBow() {

            ItemStack overseerBow = new ItemStack(ItemID.BOW);
            ItemMeta overseerMeta = overseerBow.getItemMeta();
            overseerMeta.addEnchant(Enchantment.ARROW_DAMAGE, 2, true);
            overseerMeta.addEnchant(Enchantment.ARROW_FIRE, 1, true);
            overseerMeta.setDisplayName(OVERSEER_BOW.toString());
            ((Repairable) overseerMeta).setRepairCost(400);
            overseerBow.setItemMeta(overseerMeta);
            return overseerBow;
        }

        public static ItemStack godFish(int amount) {

            ItemStack godFish = new ItemStack(ItemID.RAW_FISH, amount);
            ItemMeta fishMeta = godFish.getItemMeta();
            fishMeta.setDisplayName(GOD_FISH.toString());
            godFish.setItemMeta(fishMeta);
            return godFish;
        }

        public static ItemStack gemOfDarkness(int amount) {

            ItemStack gemOfDarkness = new ItemStack(ItemID.EMERALD, amount);
            ItemMeta gemMeta = gemOfDarkness.getItemMeta();
            gemMeta.setDisplayName(GEM_OF_DARKNESS.toString());
            gemOfDarkness.setItemMeta(gemMeta);
            return gemOfDarkness;
        }

        public static ItemStack pixieDust(int amount) {

            ItemStack phantomGold = new ItemStack(ItemID.SUGAR, amount);
            ItemMeta goldMeta = phantomGold.getItemMeta();
            goldMeta.setDisplayName(PIXIE_DUST.toString());
            phantomGold.setItemMeta(goldMeta);
            return phantomGold;
        }

        public static ItemStack phantomGold(int amount) {

            ItemStack phantomGold = new ItemStack(ItemID.GOLD_BAR, amount);
            ItemMeta goldMeta = phantomGold.getItemMeta();
            goldMeta.setDisplayName(PHANTOM_GOLD.toString());
            phantomGold.setItemMeta(goldMeta);
            return phantomGold;
        }

        public static ItemStack phantomClock(int amount) {

            ItemStack phantomClock = new ItemStack(ItemID.WATCH, amount);
            ItemMeta phantomMeta = phantomClock.getItemMeta();
            phantomMeta.setDisplayName(PHANTOM_CLOCK.toString());
            phantomClock.setItemMeta(phantomMeta);
            return phantomClock;
        }

        public static ItemStack imbuedCrystal(int amount) {

            ItemStack gemOfDarkness = new ItemStack(ItemID.DIAMOND, amount);
            ItemMeta gemMeta = gemOfDarkness.getItemMeta();
            gemMeta.setDisplayName(IMBUED_CRYSTAL.toString());
            gemOfDarkness.setItemMeta(gemMeta);
            return gemOfDarkness;
        }

        public static ItemStack gemOfLife(int amount) {

            ItemStack gemOfLife = new ItemStack(ItemID.DIAMOND, amount);
            ItemMeta gemMeta = gemOfLife.getItemMeta();
            gemMeta.setDisplayName(GEM_OF_LIFE.toString());
            gemOfLife.setItemMeta(gemMeta);
            return gemOfLife;
        }

        public static ItemStack phantomHymn() {

            ItemStack phantomHymn = new ItemStack(ItemID.BOOK);
            ItemMeta phantomMeta = phantomHymn.getItemMeta();
            phantomMeta.setDisplayName(PHANTOM_HYMN.toString());
            phantomMeta.setLore(Arrays.asList(ChatColor.RED + "A hymn of dark origins..."));
            phantomHymn.setItemMeta(phantomMeta);
            return phantomHymn;
        }

        public static ItemStack barbarianBone(int amount) {

            ItemStack barbarianBones = new ItemStack(ItemID.BONE, amount);
            ItemMeta boneMeta = barbarianBones.getItemMeta();
            boneMeta.setDisplayName(BARBARIAN_BONE.toString());
            barbarianBones.setItemMeta(boneMeta);
            return barbarianBones;
        }

        public static ItemStack batBow() {

            ItemStack batBow = new ItemStack(ItemID.BOW);
            ItemMeta batMeta = batBow.getItemMeta();
            batMeta.setDisplayName(BAT_BOW.toString());
            ((Repairable) batMeta).setRepairCost(400);
            batBow.setItemMeta(batMeta);
            return batBow;
        }

        public static ItemStack chickenBow() {

            ItemStack chickenBow = new ItemStack(ItemID.BOW);
            ItemMeta chickenMeta = chickenBow.getItemMeta();
            chickenMeta.setDisplayName(CHICKEN_BOW.toString());
            ((Repairable) chickenMeta).setRepairCost(400);
            chickenBow.setItemMeta(chickenMeta);
            return chickenBow;
        }

        public static ItemStack chickenHymn() {

            ItemStack phantomHymn = new ItemStack(ItemID.BOOK);
            ItemMeta phantomMeta = phantomHymn.getItemMeta();
            phantomMeta.setDisplayName(CHICKEN_HYMN.toString());
            phantomMeta.setLore(Arrays.asList(ChatColor.BLUE + "Cluck cluck!"));
            phantomHymn.setItemMeta(phantomMeta);
            return phantomHymn;
        }

        public static ItemStack magicBucket() {

            ItemStack magicBucket = new ItemStack(ItemID.BUCKET);
            ItemMeta magicMeta = magicBucket.getItemMeta();
            magicMeta.setDisplayName(MAGIC_BUCKET.toString());
            magicBucket.setItemMeta(magicMeta);
            return magicBucket;
        }
    }

    public static ItemStack makeSkull(String name) {

        ItemStack skull = new ItemStack(ItemID.HEAD, 1, (short) 3);
        SkullMeta skullMeta = (SkullMeta) skull.getItemMeta();
        skullMeta.setOwner(name);
        skull.setItemMeta(skullMeta);
        return skull;
    }

    public static ItemStack[] clone(ItemStack[] stacks) {

        ItemStack[] returnStack = new ItemStack[stacks.length];
        for (int i = 0; i < stacks.length; i++) {
            returnStack[i] = stacks[i] == null ? null : stacks[i].clone();
        }
        return returnStack;
    }

    public static SerializableItemStack[] serialize(ItemStack[] stacks) {

        SerializableItemStack[] returnStack = new SerializableItemStack[stacks.length];
        for (int i = 0; i < stacks.length; i++) {
            returnStack[i] = stacks[i] == null ? null : new SerializableItemStack(stacks[i]);
        }
        return returnStack;
    }

    public static ItemStack[] unSerialize(SerializableItemStack[] stacks) {

        ItemStack[] returnStack = new ItemStack[stacks.length];
        for (int i = 0; i < stacks.length; i++) {
            returnStack[i] = stacks[i] == null ? null : stacks[i].bukkitRestore();
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

    private static final int[] swords = new int[]{
            ItemID.WOOD_SWORD, ItemID.STONE_SWORD,
            ItemID.IRON_SWORD, ItemID.GOLD_SWORD,
            ItemID.DIAMOND_SWORD
    };

    public static boolean isSword(int itemId) {

        for (int sword : swords) {
            if (itemId == sword) {
                return true;
            }
        }
        return false;
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

    public static boolean hasItem(Player player, CustomItems type) {

        return player.isValid() && findItemOfName(player.getInventory().getContents(), type.toString());
    }

    public static boolean isHoldingItem(Player player, CustomItems type) {

        return player.isValid() && isItem(player.getItemInHand(), type);
    }

    public static boolean isItem(ItemStack stack, CustomItems type) {

        return matchesFilter(stack, type.toString());
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

    public static boolean hasForgeBook(Player player) {

        return player.isValid() && hasForgeBook(player.getItemInHand());
    }

    public static Map<String, String> getItemTags(ItemStack item) {

        if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasLore()) return null;

        Map<String, String> tags = new HashMap<>();
        for (String line : item.getItemMeta().getLore()) {
            String[] args = line.split(":");
            if (args.length < 2) continue;

            for (int i = 0; i < args.length; i++) {
                args[i] = args[i].trim();
            }
            tags.put(args[0], args[args.length - 1]);
        }
        return tags;
    }

    public static boolean hasForgeBook(ItemStack item) {

        return item.hasItemMeta() && item.getItemMeta() instanceof BookMeta
                && ((BookMeta) item.getItemMeta()).hasAuthor()
                && ((BookMeta) item.getItemMeta()).getAuthor().equals("The Forge Knights");
    }

    public static int fortuneLevel(ItemStack pickaxe) {

        if (!pickaxe.containsEnchantment(Enchantment.LOOT_BONUS_BLOCKS)) return 0;
        return pickaxe.getEnchantmentLevel(Enchantment.LOOT_BONUS_BLOCKS);
    }

    public static int fortuneModifier(int typeId, int fortuneLevel) {

        int returnValue = 1;
        switch (typeId) {
            case BlockID.IRON_ORE:
            case BlockID.COAL_ORE:
            case BlockID.GOLD_ORE:
            case BlockID.LAPIS_LAZULI_ORE:
            case BlockID.REDSTONE_ORE:
            case BlockID.GLOWING_REDSTONE_ORE:
            case BlockID.DIAMOND_ORE:
            case BlockID.EMERALD_ORE:
            case BlockID.QUARTZ_ORE:
                switch (fortuneLevel) {
                    case 1:
                        if (ChanceUtil.getChance(3)) {
                            returnValue *= 2;
                        }
                        break;
                    case 2:
                        if (ChanceUtil.getChance(4)) {
                            returnValue *= 3;
                        } else if (ChanceUtil.getChance(4)) {
                            returnValue *= 2;
                        }
                        break;
                    case 3:
                        if (ChanceUtil.getChance(5)) {
                            returnValue *= 4;
                        } else if (ChanceUtil.getChance(5)) {
                            returnValue *= 3;
                        } else if (ChanceUtil.getChance(5)) {
                            returnValue *= 2;
                        }
                        break;
                }
        }
        return returnValue;
    }
}
