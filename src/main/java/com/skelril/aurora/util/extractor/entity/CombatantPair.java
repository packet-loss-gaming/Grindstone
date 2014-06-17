/*
 * Copyright (c) 2014 Wyatt Childers.
 *
 * All Rights Reserved
 */

package com.skelril.aurora.util.extractor.entity;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Projectile;

public class CombatantPair<Attacker extends Entity, Defender extends Entity, Thrown extends Projectile> {

    private final Attacker attacker;
    private final Defender defender;
    private final Thrown projectile;

    public CombatantPair(Attacker attacker, Defender defender) {
        this(attacker, defender, null);
    }

    public CombatantPair(Attacker attacker, Defender defender, Thrown projectile) {
        this.attacker = attacker;
        this.defender = defender;
        this.projectile = projectile;
    }

    public Attacker getAttacker() {
        return attacker;
    }

    public Defender getDefender() {
        return defender;
    }

    public boolean hasProjectile() {
        return projectile != null;
    }

    public Thrown getProjectile() {
        return projectile;
    }
}
