/*
 * Copyright (c) 2014 Wyatt Childers.
 *
 * All Rights Reserved
 */

package com.skelril.aurora.items.specialattack.attacks.melee.fear;

import com.skelril.aurora.items.specialattack.EntityAttack;
import com.skelril.aurora.items.specialattack.attacks.melee.MeleeSpecial;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class Confuse extends EntityAttack implements MeleeSpecial {

    public Confuse(LivingEntity owner, LivingEntity target) {
        super(owner, target);
    }

    @Override
    public void activate() {

        int duration = (int) Math.min(1200, owner.getHealth() * 18);
        target.addPotionEffect(new PotionEffect(PotionEffectType.CONFUSION, duration, 1), true);

        inform("Your sword confuses its victim.");
    }
}
