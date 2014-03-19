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
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;

public class MobAttack extends LocationAttack implements RangedSpecial {

    private EntityType type;

    public MobAttack(LivingEntity owner, Location target, EntityType type) {
        super(owner, target);
        this.type = type;
    }

    @Override
    public void activate() {

        EffectUtil.Strange.mobBarrage(target, type);

        if (type == EntityType.BAT) {
            inform("Your bow releases a batty attack.");
        } else {
            inform("Your bow releases a " + type.getName().toLowerCase() + " attack.");
        }
    }
}
