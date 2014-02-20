/*
 * Copyright (c) 2014 Wyatt Childers.
 *
 * All Rights Reserved
 */

package com.skelril.aurora.prayer.PrayerFX;

import com.skelril.aurora.prayer.PrayerType;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * Author: Turtle9598
 */
public class PowerFX extends AbstractEffect {

    private static final AbstractEffect[] subFX = new AbstractEffect[]{
            new InfiniteHungerFX()
    };
    private static PotionEffect[] effects = new PotionEffect[]{
            new PotionEffect(PotionEffectType.INCREASE_DAMAGE, 20 * 600, 2),
            new PotionEffect(PotionEffectType.REGENERATION, 20 * 600, 2),
            new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 20 * 600, 2),
            new PotionEffect(PotionEffectType.WATER_BREATHING, 20 * 600, 2),
            new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 20 * 600, 2)
    };
    private static PotionEffectType[] removableEffects = new PotionEffectType[]{
            PotionEffectType.CONFUSION, PotionEffectType.BLINDNESS, PotionEffectType.WEAKNESS,
            PotionEffectType.POISON, PotionEffectType.SLOW
    };

    public PowerFX() {

        super(subFX, effects);
    }

    @Override
    public PrayerType getType() {

        return PrayerType.POWER;
    }

    @Override
    public void clean(Player player) {

        super.clean(player);

        for (PotionEffectType removableEffect : removableEffects) {
            player.removePotionEffect(removableEffect);
        }
    }
}
