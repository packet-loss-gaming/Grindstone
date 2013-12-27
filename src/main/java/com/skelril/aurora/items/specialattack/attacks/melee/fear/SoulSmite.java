package com.skelril.aurora.items.specialattack.attacks.melee.fear;

import com.skelril.aurora.items.specialattack.EntityAttack;
import com.skelril.aurora.items.specialattack.attacks.melee.MeleeSpecial;
import org.bukkit.entity.LivingEntity;

/**
 * Created by wyatt on 12/26/13.
 */
public class SoulSmite extends EntityAttack implements MeleeSpecial {

    public SoulSmite(LivingEntity owner, LivingEntity target) {
        super(owner, target);
    }

    @Override
    public void activate() {

        final double targetHP = target.getHealth() / target.getMaxHealth();

        target.setHealth((targetHP / 2) * target.getMaxHealth());
        server.getScheduler().runTaskLater(inst, new Runnable() {

            @Override
            public void run() {

                if (target.isValid()) {
                    double newTargetHP = target.getHealth() / target.getMaxHealth();
                    if (newTargetHP < targetHP) {
                        target.setHealth(target.getMaxHealth() * targetHP);
                    }
                }
                inform("Your sword releases its grasp on its victim.");
            }
        }, 20 * (int) Math.min(20, target.getMaxHealth() / 5 + 1));
        inform("Your sword steals its victims health for a short time.");
    }
}
