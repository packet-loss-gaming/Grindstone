/*
 * Copyright (c) 2014 Wyatt Childers.
 *
 * All Rights Reserved
 */

package com.skelril.aurora.items.specialattack.attacks.hybrid.fear;

import com.skelril.aurora.items.specialattack.EntityAttack;
import com.skelril.aurora.items.specialattack.attacks.melee.MeleeSpecial;
import com.skelril.aurora.items.specialattack.attacks.ranged.RangedSpecial;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class Curse extends EntityAttack implements MeleeSpecial, RangedSpecial {

    public Curse(LivingEntity owner, LivingEntity target) {
        super(owner, target);
    }

    @Override
    public void activate() {

        int duration = (int) Math.min(20 * 60 * 5, owner.getHealth() * 24);
        target.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, duration, 2), true);

        inform("Your weapon curses its victim.");
    }
}
