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

public class Weaken extends EntityAttack implements MeleeSpecial {

    public Weaken(LivingEntity owner, LivingEntity target) {
        super(owner, target);
    }

    @Override
    public void activate() {

        int duration = (int) Math.min(20 * 60 * 5, owner.getHealth() * 18);
        target.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, duration, 1), true);
        owner.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, duration, 1), true);

        inform("Your sword leaches strength from its victim.");
    }
}
