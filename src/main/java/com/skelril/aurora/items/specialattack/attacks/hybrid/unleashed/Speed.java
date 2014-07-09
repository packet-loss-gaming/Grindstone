/*
 * Copyright (c) 2014 Wyatt Childers.
 *
 * All Rights Reserved
 */

package com.skelril.aurora.items.specialattack.attacks.hybrid.unleashed;

import com.skelril.aurora.items.specialattack.EntityAttack;
import com.skelril.aurora.items.specialattack.attacks.melee.MeleeSpecial;
import com.skelril.aurora.items.specialattack.attacks.ranged.RangedSpecial;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class Speed extends EntityAttack implements MeleeSpecial, RangedSpecial {

    public Speed(LivingEntity owner, LivingEntity target) {
        super(owner, target);
    }

    @Override
    public void activate() {

        int duration = (int) Math.min(20 * 60 * 5, owner.getHealth() * 18);
        owner.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, duration, 2), true);
        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, duration, 2), true);

        inform("You gain a agile advantage over your opponent.");
    }
}
