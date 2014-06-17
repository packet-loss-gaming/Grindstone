/*
 * Copyright (c) 2014 Wyatt Childers.
 *
 * All Rights Reserved
 */

package com.skelril.aurora.util.extractor.entity;

import com.skelril.aurora.util.extractor.Extractor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.projectiles.ProjectileSource;

@SuppressWarnings("unchecked")
public class EDBEExtractor<Attacker extends Entity, Defender extends Entity, Thrown extends Projectile>
       implements Extractor<CombatantPair<Attacker, Defender, Thrown>, EntityDamageByEntityEvent> {

    private final Class<Attacker> attackerType;
    private final Class<Defender> defenderType;
    private final Class<Thrown> thrownClass;

    public EDBEExtractor(Class<Attacker> attackerType, Class<Defender> defenderType, Class<Thrown> thrownClass) {
        this.attackerType = attackerType;
        this.defenderType = defenderType;
        this.thrownClass = thrownClass;
    }

    @Override
    public CombatantPair<Attacker, Defender, Thrown> extractFrom(EntityDamageByEntityEvent event) {
        Entity defender = event.getEntity();
        Entity attacker = event.getDamager();
        Projectile projectile = null;

        if (attacker instanceof Projectile) {
            ProjectileSource shooter = ((Projectile) attacker).getShooter();
            projectile = (Projectile) attacker;
            if (shooter == null || !thrownClass.isInstance(projectile)) {
                return null;
            }
            attacker = (Entity) shooter;
        }

        if (defenderType.isInstance(defender) && attackerType.isInstance(attacker)) {
            if (projectile == null) {
                return new CombatantPair<>((Attacker) attacker, (Defender) defender);
            }
            return new CombatantPair<>((Attacker) attacker, (Defender) defender, (Thrown) projectile);
        }
        return null;
    }
}
