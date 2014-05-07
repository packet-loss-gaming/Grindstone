/*
 * Copyright (c) 2014 Wyatt Childers.
 *
 * All Rights Reserved
 */

package com.skelril.aurora.util.item.custom;

import org.bukkit.potion.PotionEffectType;

public class Potion {
    private PotionEffectType type;
    private int time;
    private int level;

    public Potion(PotionEffectType type, int time, int level) {
        this.type = type;
        this.time = time;
        this.level = level;
    }

    public PotionEffectType getType() {
        return type;
    }

    public int getTime() {
        return time;
    }

    public int getLevel() {
        return level;
    }
}
