package com.skelril.aurora.items.specialattack.attacks.melee.fear;

import com.skelril.aurora.items.specialattack.EntityAttack;
import com.skelril.aurora.items.specialattack.attacks.melee.MeleeSpecial;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * Created by wyatt on 12/26/13.
 */
public class Weaken extends EntityAttack implements MeleeSpecial {

    public Weaken(LivingEntity owner, LivingEntity target) {
        super(owner, target);
    }

    @Override
    public void activate() {

        target.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, (int) (owner.getHealth() * 18), 1), true);
        owner.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, (int) (owner.getHealth() * 18), 1), true);

        inform("Your sword leaches strength from its victim.");
    }
}
