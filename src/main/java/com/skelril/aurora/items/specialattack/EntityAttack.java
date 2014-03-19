/*
 * Copyright (c) 2014 Wyatt Childers.
 *
 * All Rights Reserved
 */

package com.skelril.aurora.items.specialattack;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

public abstract class EntityAttack extends SpecialAttack {

    protected LivingEntity target;

    public EntityAttack(LivingEntity owner, LivingEntity target) {
        super(owner);
        this.target = target;
    }

    @Override
    public LivingEntity getTarget() {

        return target;
    }

    @Override
    public Location getLocation() {

        return target.getLocation();
    }
}
