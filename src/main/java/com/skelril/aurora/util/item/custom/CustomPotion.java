/*
 * Copyright (c) 2014 Wyatt Childers.
 *
 * All Rights Reserved
 */

package com.skelril.aurora.util.item.custom;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.List;

public class CustomPotion extends CustomItem {
    List<Potion> effects = new ArrayList<>();

    public CustomPotion(CustomItems item, ItemStack base) {
        super(item, base);
        assert base.getType() == Material.POTION;
    }

    public void addEffect(Potion effect) {
        effects.add(effect);
    }

    public void addEffect(PotionEffectType type, int time, int level) {
        addEffect(new Potion(type, time, level));
    }

    public List<Potion> getEffects() {
        return effects;
    }

    @Override
    public ItemStack build() {
        ItemStack base = super.build();
        PotionMeta meta = (PotionMeta) base.getItemMeta();
        for (Potion potion : effects) {
            meta.addCustomEffect(new PotionEffect(potion.getType(), potion.getTime(), potion.getLevel()), false);
        }
        base.setItemMeta(meta);
        return base;
    }
}
