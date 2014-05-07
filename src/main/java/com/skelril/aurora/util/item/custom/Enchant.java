/*
 * Copyright (c) 2014 Wyatt Childers.
 *
 * All Rights Reserved
 */

package com.skelril.aurora.util.item.custom;

import org.bukkit.enchantments.Enchantment;

public class Enchant {

    private Enchantment enchant;
    private int level;

    public Enchant(Enchantment enchant, int level) {
        this.enchant = enchant;
        this.level = level;
    }

    public org.bukkit.enchantments.Enchantment getEnchant() {
        return enchant;
    }

    public int getLevel() {
        return level;
    }
}
