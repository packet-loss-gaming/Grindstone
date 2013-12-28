package com.skelril.aurora.items.specialattack.attacks.melee.fear;

import com.skelril.aurora.events.anticheat.RapidHitEvent;
import com.skelril.aurora.items.specialattack.EntityAttack;
import com.skelril.aurora.items.specialattack.attacks.melee.MeleeSpecial;
import com.skelril.aurora.util.ChanceUtil;
import com.skelril.aurora.util.DamageUtil;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

/**
 * Created by wyatt on 12/26/13.
 */
public class Decimate extends EntityAttack implements MeleeSpecial {

    public Decimate(LivingEntity owner, LivingEntity target) {
        super(owner, target);
    }

    @Override
    public void activate() {

        if (owner instanceof Player) {
            server.getPluginManager().callEvent(new RapidHitEvent((Player) owner));
        }

        DamageUtil.damage(owner, target, ChanceUtil.getRandom(target instanceof Player ? 3 : 10) * 50);

        inform("Your sword tears through the flesh of its victim.");
    }
}
