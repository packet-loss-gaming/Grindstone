package com.skelril.aurora.items.specialattack.attacks.melee.unleashed;

import com.skelril.aurora.items.specialattack.EntityAttack;
import com.skelril.aurora.items.specialattack.attacks.melee.MeleeSpecial;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * Created by wyatt on 12/26/13.
 */
public class Regen extends EntityAttack implements MeleeSpecial {

    public Regen(LivingEntity owner, LivingEntity target) {
        super(owner, target);
    }

    @Override
    public void activate() {

        owner.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, (int) (target.getHealth() * 10), 2), true);

        inform("You gain a healing aura.");
    }
}
