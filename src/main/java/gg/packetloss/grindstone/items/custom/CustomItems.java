/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.items.custom;

import org.bukkit.ChatColor;

public enum CustomItems {

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
    MASTER_SWORD(ChatColor.DARK_PURPLE, "Master Sword"),
    MASTER_BOW(ChatColor.DARK_PURPLE, "Master Bow"),

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

    // Ninja Guild
    NINJA_STAR(ChatColor.DARK_GRAY, "Ninja Star"),

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
    TOME_OF_THE_RIFT_SPLITTER(ChatColor.BLACK, "Tome of the Rift Splitter");

    private WeaponFamily family;
    private ChatColor color;
    private String name;

    private CustomItems(WeaponFamily family, String kind) {
        this.family = family;
        this.color = family.getColor();
        this.name = family.getProperName() + " " + kind;
    }

    private CustomItems(ChatColor color, String name) {

        this.color = color;
        this.name = name;
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

    @Override
    public String toString() {
        return color + name;
    }
}
