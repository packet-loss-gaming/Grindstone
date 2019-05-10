/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.util.extractor.entity;

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
