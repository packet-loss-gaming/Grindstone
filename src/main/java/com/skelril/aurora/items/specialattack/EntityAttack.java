package com.skelril.aurora.items.specialattack;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

/**
 * Created by wyatt on 12/26/13.
 */
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
