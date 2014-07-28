/*
 * Copyright (c) 2014 Wyatt Childers.
 *
 * All Rights Reserved
 */

package com.skelril.aurora.items.specialattack.attacks.ranged.misc;

import com.skelril.aurora.items.specialattack.LocationAttack;
import com.skelril.aurora.items.specialattack.attacks.ranged.RangedSpecial;
import com.skelril.aurora.util.item.EffectUtil;
import org.bukkit.Location;
import org.bukkit.entity.Bat;
import org.bukkit.entity.LivingEntity;

public class MobAttack extends LocationAttack implements RangedSpecial {

    private Class<? extends LivingEntity> type;

    public <T extends LivingEntity> MobAttack(LivingEntity owner, Location target, Class<T> type) {
        super(owner, target);
        this.type = type;
    }

    @Override
    public void activate() {

        EffectUtil.Strange.mobBarrage(target, type);

        if (Bat.class.equals(type)) {
            inform("Your bow releases a batty attack.");
        } else {
            inform("Your bow releases a " + type.getSimpleName().toLowerCase() + " attack.");
        }
    }
}
