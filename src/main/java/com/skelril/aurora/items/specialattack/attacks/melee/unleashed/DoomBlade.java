/*
 * Copyright (c) 2014 Wyatt Childers.
 *
 * All Rights Reserved
 */

package com.skelril.aurora.items.specialattack.attacks.melee.unleashed;

import com.skelril.aurora.city.engine.PvPComponent;
import com.skelril.aurora.events.anticheat.RapidHitEvent;
import com.skelril.aurora.items.specialattack.EntityAttack;
import com.skelril.aurora.items.specialattack.attacks.melee.MeleeSpecial;
import com.skelril.aurora.util.ChanceUtil;
import com.skelril.aurora.util.DamageUtil;
import org.bukkit.Effect;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Created by wyatt on 12/26/13.
 */
public class DoomBlade extends EntityAttack implements MeleeSpecial {

    public DoomBlade(LivingEntity owner, LivingEntity target) {
        super(owner, target);
    }

    @Override
    public void activate() {

        inform("Your weapon releases a huge burst of energy.");

        if (owner instanceof Player) {
            server.getPluginManager().callEvent(new RapidHitEvent((Player) owner));
        }

        double dmgTotal = 0;
        List<Entity> entityList = target.getNearbyEntities(6, 4, 6);
        entityList.add(target);
        for (Entity e : entityList) {
            if (e.isValid() && e instanceof LivingEntity) {
                if (e.equals(owner)) continue;
                double maxHit = ChanceUtil.getRangedRandom(150, 350);
                if (e instanceof Player) {
                    if (owner instanceof Player && !PvPComponent.allowsPvP((Player) owner, (Player) e)) {
                        continue;
                    }
                    maxHit = (1.0 / 3.0) * maxHit;
                }
                DamageUtil.damage(owner, target, maxHit);
                for (int i = 0; i < 20; i++) e.getWorld().playEffect(e.getLocation(), Effect.MOBSPAWNER_FLAMES, 0);
                dmgTotal += maxHit;
            }
        }
        inform("Your sword dishes out an incredible " + (int) Math.ceil(dmgTotal) + " damage!");
    }
}
