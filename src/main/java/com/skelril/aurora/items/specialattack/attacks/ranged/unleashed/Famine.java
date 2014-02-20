/*
 * Copyright (c) 2014 Wyatt Childers.
 *
 * All Rights Reserved
 */

package com.skelril.aurora.items.specialattack.attacks.ranged.unleashed;

import com.skelril.aurora.items.specialattack.EntityAttack;
import com.skelril.aurora.items.specialattack.attacks.melee.MeleeSpecial;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

/**
 * Created by wyatt on 12/26/13.
 */
public class Famine extends EntityAttack implements MeleeSpecial {

    public Famine(LivingEntity owner, LivingEntity target) {
        super(owner, target);
    }

    @Override
    public void activate() {

        if (target instanceof Player) {
            ((Player) target).setFoodLevel((int) (((Player) target).getFoodLevel() * .85));
            ((Player) target).setSaturation(0);
        } else {
            target.setMaxHealth(target.getMaxHealth() * .9);
        }

        inform("You drain the stamina of your foe.");
    }
}
