/*
 * Copyright (c) 2014 Wyatt Childers.
 *
 * All Rights Reserved
 */

package com.skelril.aurora.items.specialattack.attacks.ranged.fear;

import com.skelril.aurora.city.engine.PvPComponent;
import com.skelril.aurora.events.anticheat.RapidHitEvent;
import com.skelril.aurora.events.anticheat.ThrowPlayerEvent;
import com.skelril.aurora.items.specialattack.EntityAttack;
import com.skelril.aurora.items.specialattack.attacks.ranged.RangedSpecial;
import com.skelril.aurora.util.ChanceUtil;
import com.skelril.aurora.util.DamageUtil;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.List;

public class FearStrike extends EntityAttack implements RangedSpecial {

    public FearStrike(LivingEntity owner, LivingEntity target) {
        super(owner, target);
    }

    @Override
    public void activate() {

        if (owner instanceof Player) {
            server.getPluginManager().callEvent(new RapidHitEvent((Player) owner));
        }

        List<Entity> entityList = target.getNearbyEntities(8, 4, 8);
        entityList.add(target);
        for (Entity e : entityList) {
            if (e.isValid() && e instanceof LivingEntity) {
                if (e.equals(owner)) continue;
                if (e instanceof Player) {
                    if (owner instanceof Player && !PvPComponent.allowsPvP((Player) owner, (Player) e)) {
                        continue;
                    }
                    server.getPluginManager().callEvent(new ThrowPlayerEvent((Player) e));
                }

                Vector velocity = owner.getLocation().getDirection().multiply(2);
                velocity.setY(Math.max(velocity.getY(), Math.random() * 2 + 1.27));
                e.setVelocity(velocity);
                DamageUtil.damage(owner, (LivingEntity) e, 10);
                e.setFireTicks(20 * (ChanceUtil.getRandom(40) + 20));
            }
        }
        inform("You fire a terrifyingly powerful shot.");
    }
}
