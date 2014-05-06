/*
 * Copyright (c) 2014 Wyatt Childers.
 *
 * All Rights Reserved
 */

package com.skelril.aurora.util.item;

import org.bukkit.ChatColor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public enum CustomItems {

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

    // Necros Armor
    NECROS_HELMET(ChatColor.DARK_RED, "Necros Helmet",
            Arrays.asList("Set Effect: Necros Armor"),
            Arrays.asList("Patient X",
                    "Market")
    ),
    NECROS_CHESTPLATE(ChatColor.DARK_RED, "Necros Chestplate",
            Arrays.asList("Set Effect: Necros Armor"),
            Arrays.asList("Patient X",
                    "Market")
    ),
    NECROS_LEGGINGS(ChatColor.DARK_RED, "Necros Leggings",
            Arrays.asList("Set Effect: Necros Armor"),
            Arrays.asList("Patient X",
                    "Market")
    ),
    NECROS_BOOTS(ChatColor.DARK_RED, "Necros Boots",
            Arrays.asList("Set Effect: Necros Armor"),
            Arrays.asList("Patient X",
                    "Market")
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
            List<String> sacSrcs = Arrays.asList(
                    "Enchanted Forest",
                    "Drop Parties",
                    "Vineam Prison",
                    "Giant Boss",
                    "Wilderness Mobs",
                    "Sacrificial Pit"
            );
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
