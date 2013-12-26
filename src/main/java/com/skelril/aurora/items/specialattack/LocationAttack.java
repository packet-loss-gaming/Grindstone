package com.skelril.aurora.items.specialattack;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

/**
 * Created by wyatt on 12/26/13.
 */
public abstract class LocationAttack extends SpecialAttack {

    protected Location target;

    public LocationAttack(LivingEntity owner, Location target) {
        super(owner);
        this.target = target;
    }

    @Override
    public LivingEntity getTarget() {

        return null;
    }

    @Override
    public Location getLocation() {

        return target;
    }
}
