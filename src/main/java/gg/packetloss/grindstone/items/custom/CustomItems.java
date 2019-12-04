/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.items.custom;

import org.bukkit.ChatColor;

public enum CustomItems {
    // Admin Weapons
    PWNG_SHORT_SWORD(WeaponFamily.PWNG, "Short Sword"),
    PWNG_BOW(WeaponFamily.PWNG, "Bow"),

    // Ancient Armor
    ANCIENT_CROWN(ChatColor.GOLD, "Ancient Crown"),
    ANCIENT_HELMET(ChatColor.GOLD, "Ancient Helmet"),
    ANCIENT_CHESTPLATE(ChatColor.GOLD, "Ancient Chestplate"),
    ANCIENT_LEGGINGS(ChatColor.GOLD, "Ancient Leggings"),
    ANCIENT_BOOTS(ChatColor.GOLD, "Ancient Boots"),

    // Nectric Armor
    NECTRIC_HELMET(ChatColor.DARK_RED, "Nectric Helmet"),
    NECTRIC_CHESTPLATE(ChatColor.DARK_RED, "Nectric Chestplate"),
    NECTRIC_LEGGINGS(ChatColor.DARK_RED, "Nectric Leggings"),
    NECTRIC_BOOTS(ChatColor.DARK_RED, "Nectric Boots"),

    // Necros Armor
    NECROS_HELMET(ChatColor.DARK_RED, "Necros Helmet"),
    NECROS_CHESTPLATE(ChatColor.DARK_RED, "Necros Chestplate"),
    NECROS_LEGGINGS(ChatColor.DARK_RED, "Necros Leggings"),
    NECROS_BOOTS(ChatColor.DARK_RED, "Necros Boots"),

    // Master Weapons
    MASTER_SWORD(WeaponFamily.MASTER, "Sword"),
    MASTER_SHORT_SWORD(WeaponFamily.MASTER, "Short Sword"),
    MASTER_BOW(WeaponFamily.MASTER, "Bow"),

    // Unleashed Weapons
    UNLEASHED_SWORD(WeaponFamily.UNLEASHED, "Sword"),
    UNLEASHED_SHORT_SWORD(WeaponFamily.UNLEASHED, "Short Sword"),
    UNLEASHED_BOW(WeaponFamily.UNLEASHED, "Bow"),

    // Fear Weapons
    FEAR_SWORD(WeaponFamily.FEAR, "Sword"),
    FEAR_SHORT_SWORD(WeaponFamily.FEAR, "Short Sword"),
    FEAR_BOW(WeaponFamily.FEAR, "Bow"),

    // Shadow Items
    SHADOW_SWORD(ChatColor.DARK_RED, "Shadow Sword"),
    SHADOW_BOW(ChatColor.DARK_RED, "Shadow Bow"),

    // Red Items
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
    DIVINE_COMBAT_POTION(ChatColor.WHITE, "Divine Combat Potion"),
    HOLY_COMBAT_POTION(ChatColor.WHITE, "Holy Combat Potion"),
    EXTREME_COMBAT_POTION(ChatColor.WHITE, "Extreme Combat Potion"),

    // Grave Yard Gems
    GEM_OF_LIFE(ChatColor.DARK_AQUA, "Gem of Life"),
    GEM_OF_DARKNESS(ChatColor.DARK_RED, "Gem of Darkness"),
    IMBUED_CRYSTAL(ChatColor.AQUA, "Imbued Crystal"),

    // Phantom Items
    PHANTOM_GOLD(ChatColor.GOLD, "Phantom Gold"),
    PHANTOM_CLOCK(ChatColor.DARK_RED, "Phantom Clock"),
    PHANTOM_HYMN(ChatColor.DARK_RED, "Phantom Hymn"),

    // Linear Tools
    LINEAR_PICKAXE(ChatColor.DARK_GREEN, "Linear Pickaxe"),
    LINEAR_SHOVEL(ChatColor.DARK_GREEN, "Linear Shovel"),
    LINEAR_AXE(ChatColor.DARK_GREEN, "Linear Axe"),

    // Radial Tools
    RADIAL_PICKAXE(ChatColor.DARK_GREEN, "Radial Pickaxe"),
    RADIAL_SHOVEL(ChatColor.DARK_GREEN, "Radial Shovel"),
    RADIAL_AXE(ChatColor.DARK_GREEN, "Radial Axe"),

    // Ninja Guild
    NINJA_STAR(ChatColor.DARK_GRAY, "Ninja Star"),

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

    // Miscellaneous
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
    TOME_OF_LEGENDS(ChatColor.GOLD, "Tome of Legends");

    private final WeaponFamily family;
    private final ChatColor color;
    private final String name;
    private final String namespaceName;

    CustomItems(WeaponFamily family, String kind) {
        this.family = family;
        this.color = family.getColor();
        this.name = family.getProperName() + " " + kind;
        this.namespaceName = computeNamespaceName(this.name);
    }

    CustomItems(ChatColor color, String name) {
        this.family = null;
        this.color = color;
        this.name = name;
        this.namespaceName = computeNamespaceName(this.name);
    }

    public WeaponFamily getFamily() {
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

    public static String computeNamespaceName(String name) {
        return "grindstone:" + name.toLowerCase().replaceAll("'s", "").replaceAll(" ", "_");
    }

    @Override
    public String toString() {
        return color + name;
    }
}
