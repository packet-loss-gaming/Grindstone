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

public class FearBlaze extends EntityAttack implements MeleeSpecial {

    public FearBlaze(LivingEntity owner, LivingEntity target) {
        super(owner, target);
    }

    @Override
    public void activate() {

        target.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, (int) (target.getHealth() * 20), 0), true);
        target.setFireTicks((int) (owner.getHealth() * 20));

        inform("Your sword releases a deadly blaze.");
    }
}
