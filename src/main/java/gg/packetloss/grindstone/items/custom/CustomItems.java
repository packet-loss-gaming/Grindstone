/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.items.custom;

import org.bukkit.ChatColor;

public enum CustomItems {
    // Admin Weapons
    PWNG_SHORT_SWORD(ItemFamily.PWNG, "Short Sword"),
    PWNG_BOW(ItemFamily.PWNG, "Bow"),

    // Ancient Armor
    ANCIENT_CROWN(ItemFamily.ANCIENT, "Crown"),
    ANCIENT_ROYAL_HELMET(ItemFamily.ANCIENT_ROYAL, "Helmet"),
    ANCIENT_ROYAL_CHESTPLATE(ItemFamily.ANCIENT_ROYAL, "Chestplate"),
    ANCIENT_ROYAL_LEGGINGS(ItemFamily.ANCIENT_ROYAL, "Leggings"),
    ANCIENT_ROYAL_BOOTS(ItemFamily.ANCIENT_ROYAL, "Boots"),
    ANCIENT_HELMET(ItemFamily.ANCIENT, "Helmet"),
    ANCIENT_CHESTPLATE(ItemFamily.ANCIENT, "Chestplate"),
    ANCIENT_LEGGINGS(ItemFamily.ANCIENT, "Leggings"),
    ANCIENT_BOOTS(ItemFamily.ANCIENT, "Boots"),

    // Nectric Armor
    NECTRIC_HELMET(ItemFamily.NETRIC, "Helmet"),
    NECTRIC_CHESTPLATE(ItemFamily.NETRIC, "Chestplate"),
    NECTRIC_LEGGINGS(ItemFamily.NETRIC, "Leggings"),
    NECTRIC_BOOTS(ItemFamily.NETRIC, "Boots"),

    // Necros Armor
    NECROS_HELMET(ItemFamily.NECROS, "Helmet"),
    NECROS_CHESTPLATE(ItemFamily.NECROS, "Chestplate"),
    NECROS_LEGGINGS(ItemFamily.NECROS, "Leggings"),
    NECROS_BOOTS(ItemFamily.NECROS, "Boots"),

    // Master Weapons
    MASTER_SWORD(ItemFamily.MASTER, "Sword"),
    MASTER_SHORT_SWORD(ItemFamily.MASTER, "Short Sword"),
    MASTER_BOW(ItemFamily.MASTER, "Bow"),

    // Unleashed Weapons
    UNLEASHED_SWORD(ItemFamily.UNLEASHED, "Sword"),
    UNLEASHED_SHORT_SWORD(ItemFamily.UNLEASHED, "Short Sword"),
    UNLEASHED_BOW(ItemFamily.UNLEASHED, "Bow"),

    // Fear Weapons
    FEAR_SWORD(ItemFamily.FEAR, "Sword"),
    FEAR_SHORT_SWORD(ItemFamily.FEAR, "Short Sword"),
    FEAR_BOW(ItemFamily.FEAR, "Bow"),

    // Shadow Items
    SHADOW_SWORD(ChatColor.DARK_RED, "Shadow Sword"),
    SHADOW_BOW(ChatColor.DARK_RED, "Shadow Bow"),

    // Red Items
    RED_SWORD(ChatColor.DARK_RED, "Red Sword"),
    RED_BOW(ChatColor.DARK_RED, "Red Bow"),
    RED_FEATHER(ChatColor.DARK_RED, "Red Feather"),

    // God Weapons
    GOD_SWORD(ChatColor.RED, "God Sword"),
    GOD_SHORT_SWORD(ChatColor.RED, "God Short Sword"),
    GOD_BOW(ChatColor.RED, "God Bow"),

    // God Tools
    GOD_AXE(ChatColor.GREEN, "God Axe"),
    LEGENDARY_GOD_AXE(ChatColor.GREEN, "Legendary God Axe"),
    GOD_PICKAXE(ChatColor.GREEN, "God Pickaxe"),
    LEGENDARY_GOD_PICKAXE(ChatColor.GREEN, "Legendary God Pickaxe"),

    // God Armor
    GOD_HELMET(ChatColor.BLUE, "God Helmet"),
    GOD_CHESTPLATE(ChatColor.BLUE, "God Chestplate"),
    GOD_LEGGINGS(ChatColor.BLUE, "God Leggings"),
    GOD_BOOTS(ChatColor.BLUE, "God Boots"),

    // Combat Potions
    DIVINE_COMBAT_POTION(ChatColor.RED, "Divine Combat Potion"),
    HOLY_COMBAT_POTION(ChatColor.RED, "Holy Combat Potion"),
    EXTREME_COMBAT_POTION(ChatColor.RED, "Extreme Combat Potion"),

    // Grave Yard Gems
    GEM_OF_LIFE(ChatColor.DARK_AQUA, "Gem of Life"),
    GEM_OF_DARKNESS(ChatColor.DARK_RED, "Gem of Darkness"),
    IMBUED_CRYSTAL(ChatColor.AQUA, "Imbued Crystal"),

    // Peaceful Warrior Armor
    PEACEFUL_WARRIOR_HELMET(ItemFamily.PEACEFUL_WARRIOR, "Helmet"),
    PEACEFUL_WARRIOR_CHESTPLATE(ItemFamily.PEACEFUL_WARRIOR, "Chestplate"),
    PEACEFUL_WARRIOR_LEGGINGS(ItemFamily.PEACEFUL_WARRIOR, "Leggings"),
    PEACEFUL_WARRIOR_BOOTS(ItemFamily.PEACEFUL_WARRIOR, "Boots"),

    // Phantom Items
    PHANTOM_GOLD(ChatColor.GOLD, "Phantom Gold"),
    PHANTOM_DIAMOND(ChatColor.GOLD, "Phantom Diamond"),
    PHANTOM_SABRE(ChatColor.DARK_RED, "Phantom Sabre"),
    PHANTOM_LINK(ChatColor.DARK_RED, "Phantom Link"),
    PHANTOM_CLOCK(ChatColor.DARK_RED, "Phantom Clock"),
    PHANTOM_HYMN(ChatColor.DARK_RED, "Phantom Hymn"),
    PHANTOM_POTION(ChatColor.DARK_RED, "Phantom Potion"),
    NEWBIE_PHANTOM_POTION(ChatColor.DARK_RED, "Newbie Phantom Potion", true),
    PHANTOM_ESSENCE(ChatColor.DARK_RED, "Phantom Essence"),

    // Linear Tools
    LINEAR_PICKAXE(ChatColor.DARK_GREEN, "Linear Pickaxe"),
    LINEAR_SHOVEL(ChatColor.DARK_GREEN, "Linear Shovel"),
    LINEAR_AXE(ChatColor.DARK_GREEN, "Linear Axe"),
    LINEAR_BLOCK_PLACER(ChatColor.DARK_GREEN, "Linear Block Placer"),

    // Radial Tools
    RADIAL_PICKAXE(ChatColor.DARK_GREEN, "Radial Pickaxe"),
    RADIAL_SHOVEL(ChatColor.DARK_GREEN, "Radial Shovel"),
    RADIAL_AXE(ChatColor.DARK_GREEN, "Radial Axe"),

    // Ninja Guild
    NINJA_STAR(ChatColor.DARK_GRAY, "Ninja Star"),
    NINJA_OATH(ChatColor.DARK_BLUE, "Ninja Oath"),

    // Rogue Guild
    ROGUE_OATH(ChatColor.DARK_RED, "Rogue Oath"),

    // Magical Items
    ODE_TO_THE_FROZEN_KING(ChatColor.AQUA, "Ode to The Frozen King"),

    // Flight Items
    PIXIE_DUST(ChatColor.GOLD, "Pixie Dust"),
    MAGIC_BUCKET(ChatColor.DARK_PURPLE, "Magic Bucket"),

    // Animal Bow
    BAT_BOW(ChatColor.DARK_RED, "Bat Bow"),
    CHICKEN_BOW(ChatColor.GOLD, "Chicken Bow"),

    // Animal Hymn
    CHICKEN_HYMN(ChatColor.GOLD, "Chicken Hymn"),

    // Summation Stuff
    SCROLL_OF_SUMMATION(ChatColor.GOLD, "Scroll of Summation"),
    HYMN_OF_SUMMATION(ChatColor.GOLD, "Hymn of Summation"),

    // Party Box
    WHITE_PARTY_BOX(ItemFamily.PARTY_BOX, "White"),
    ORANGE_PARTY_BOX(ItemFamily.PARTY_BOX, "Orange"),
    MAGENTA_PARTY_BOX(ItemFamily.PARTY_BOX, "Magenta"),
    LIGHT_BLUE_PARTY_BOX(ItemFamily.PARTY_BOX, "Light Blue"),
    YELLOW_PARTY_BOX(ItemFamily.PARTY_BOX, "Yellow"),
    LIME_PARTY_BOX(ItemFamily.PARTY_BOX, "Lime"),
    PINK_PARTY_BOX(ItemFamily.PARTY_BOX, "Pink"),
    GRAY_PARTY_BOX(ItemFamily.PARTY_BOX, "Gray"),
    LIGHT_GRAY_PARTY_BOX(ItemFamily.PARTY_BOX, "Light Gray"),
    CYAN_PARTY_BOX(ItemFamily.PARTY_BOX, "Cyan"),
    PURPLE_PARTY_BOX(ItemFamily.PARTY_BOX, "Purple"),
    BLUE_PARTY_BOX(ItemFamily.PARTY_BOX, "Blue"),
    BROWN_PARTY_BOX(ItemFamily.PARTY_BOX, "Brown"),
    GREEN_PARTY_BOX(ItemFamily.PARTY_BOX, "Green"),
    RED_PARTY_BOX(ItemFamily.PARTY_BOX, "Red"),
    BLACK_PARTY_BOX(ItemFamily.PARTY_BOX, "Black"),

    // Miscellaneous
    EXECUTIONER_AXE(ChatColor.GOLD, "Executioner's Axe"),
    MAD_MILK(ChatColor.DARK_AQUA, "Mad Milk"),
    GOD_FISH(ChatColor.BLUE, "God Fish"),
    OVERSEER_BOW(ChatColor.RED, "Overseer's Bow"),
    BARBARIAN_BONE(ChatColor.DARK_RED, "Barbarian Bone"),
    POTION_OF_RESTITUTION(ChatColor.DARK_RED, "Potion of Restitution"),
    CALMING_CRYSTAL(ChatColor.GOLD, "Calming Crystal"),
    PATIENT_X_THERAPY_NOTES(ChatColor.YELLOW, "Patient X's Therapy Notes"),
    HYMN_OF_HARVEST(ChatColor.DARK_GREEN, "Hymn of Harvest"),
    TOME_OF_THE_RIFT_SPLITTER(ChatColor.BLACK, "Tome of the Rift Splitter"),
    TOME_OF_CURSED_SMELTING(ChatColor.GOLD, "Tome of Cursed Smelting"),
    TOME_OF_POISON(ChatColor.DARK_GREEN, "Tome of Poison"),
    TOME_OF_THE_CLEANLY(ChatColor.BLUE, "Tome of the Cleanly"),
    TOME_OF_SACRIFICE(ChatColor.BLUE, "Tome of Sacrifice"),
    TOME_OF_DIVINITY(ChatColor.GOLD, "Tome of Divinity"),
    TOME_OF_THE_UNDEAD(ChatColor.DARK_RED, "Tome of the Undead"),
    TOME_OF_LEGENDS(ChatColor.GOLD, "Tome of Legends"),
    TOME_OF_LIFE(ChatColor.DARK_AQUA, "Tome of Life");

    private final ItemFamily family;
    private final ChatColor color;
    private final String name;
    private final String namespaceName;
    private final boolean expires;

    CustomItems(ItemFamily family, String kind) {
        this(family, family.getColor(), family.getProperName() + " " + kind, false);
    }

    CustomItems(ChatColor color, String name) {
        this(null, color, name, false);
    }

    CustomItems(ChatColor color, String name, boolean expires) {
        this(null, color, name, expires);
    }

    CustomItems(ItemFamily family, ChatColor color, String name, boolean expires) {
        this.family = family;
        this.color = color;
        this.name = name;
        this.namespaceName = computeNamespaceName(this.name);
        this.expires = expires;
    }

    public ItemFamily getFamily() {
        return family;
    }

    public ChatColor getColor() {
        return color;
    }

    public String getName() {
        return name;
    }

    public String getColoredName() {
        return color + name;
    }

    public String getSnakecaseName() {
        return this.name().toLowerCase();
    }

    public String getNamespaceName() {
        return namespaceName;
    }

    public boolean hasExpiration() {
        return expires;
    }

    public int getModelId() {
        return CustomItemCenter.getModelId(this);
    }

    public static String computeNamespaceName(String name) {
        return "grindstone:" + name.toLowerCase().replaceAll("'s", "").replaceAll(" ", "_");
    }

    @Override
    public String toString() {
        return color + name;
    }
}
