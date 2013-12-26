package com.skelril.aurora.items.specialattack.attacks.hybrid.unleashed;

import com.skelril.aurora.items.specialattack.EntityAttack;
import com.skelril.aurora.items.specialattack.attacks.melee.MeleeSpecial;
import com.skelril.aurora.items.specialattack.attacks.ranged.RangedSpecial;
import org.bukkit.entity.LivingEntity;

/**
 * Created by wyatt on 12/26/13.
 */
public class LifeLeech extends EntityAttack implements MeleeSpecial, RangedSpecial {

    public LifeLeech(LivingEntity owner, LivingEntity target) {
        super(owner, target);
    }

    @Override
    public void activate() {

        final double ownerMax = owner.getMaxHealth();

        final double ownerHP = owner.getHealth() / ownerMax;
        final double targetHP = target.getHealth() / target.getMaxHealth();

        if (ownerHP > targetHP) {
            owner.setHealth(Math.min(ownerMax, ownerMax * (ownerHP + .1)));
            inform("Your weapon heals you.");
        } else {
            target.setHealth(target.getMaxHealth() * ownerHP);
            owner.setHealth(Math.min(ownerMax, ownerMax * targetHP * 1.1));
            inform("You leech the health of your foe.");
        }
    }
}
